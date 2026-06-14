package com.pharmaflow.smartfeatures.service.notification;

import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderStatusRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderResponseDto;
import com.pharmaflow.smartfeatures.dto.userhealth.FamilyMemberSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.notification.TherapyReminderMapper;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import com.pharmaflow.smartfeatures.security.AuthenticatedUser;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TherapyReminderService {

  private static final LocalTime DAILY_WINDOW_START = LocalTime.of(8, 0);
  private static final LocalTime DAILY_WINDOW_END = LocalTime.of(22, 0);

  private final TherapyReminderRepository therapyReminderRepository;
  private final TherapyReminderMapper therapyReminderMapper;
  private final UserHealthClient userHealthClient;
  private final ProductCatalogClient productCatalogClient;
  private final Clock clock;

  public TherapyReminderService(
      TherapyReminderRepository therapyReminderRepository,
      TherapyReminderMapper therapyReminderMapper,
      UserHealthClient userHealthClient,
      ProductCatalogClient productCatalogClient,
      Clock clock) {
    this.therapyReminderRepository = therapyReminderRepository;
    this.therapyReminderMapper = therapyReminderMapper;
    this.userHealthClient = userHealthClient;
    this.productCatalogClient = productCatalogClient;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<TherapyReminderResponseDto> getReminders(Long patientProfileId) {
    return (patientProfileId != null
            ? therapyReminderRepository.findByPatientProfileIdOrderByStartDateDesc(patientProfileId)
            : therapyReminderRepository.findAllByOrderByStartDateDesc())
        .stream().map(therapyReminderMapper::toResponseDto).toList();
  }

  @Transactional(readOnly = true)
  public List<TherapyReminderResponseDto> getReminders(
      Long patientProfileId, AuthenticatedUser user, boolean admin) {
    if (admin) {
      return getReminders(patientProfileId);
    }

    Long ownerUserId = requireUserId(user);
    if (patientProfileId != null) {
      validateManagedPatientProfile(ownerUserId, patientProfileId);
      return therapyReminderRepository
          .findByOwnerUserIdAndPatientProfileIdOrderByStartDateDesc(ownerUserId, patientProfileId)
          .stream()
          .map(therapyReminderMapper::toResponseDto)
          .toList();
    }

    return therapyReminderRepository.findByOwnerUserIdOrderByStartDateDesc(ownerUserId).stream()
        .map(therapyReminderMapper::toResponseDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public TherapyReminderResponseDto getReminder(Long id) {
    return therapyReminderMapper.toResponseDto(findReminderById(id));
  }

  @Transactional(readOnly = true)
  public TherapyReminderResponseDto getReminder(Long id, AuthenticatedUser user, boolean admin) {
    TherapyReminder reminder = findReminderById(id);
    validateReminderOwner(reminder, user, admin);
    return therapyReminderMapper.toResponseDto(reminder);
  }

  @Transactional
  public TherapyReminderResponseDto createReminder(TherapyReminderRequestDto requestDto) {
    throw new AccessDeniedException("Authenticated user id is required.");
  }

  @Transactional
  public TherapyReminderResponseDto createReminder(
      TherapyReminderRequestDto requestDto, AuthenticatedUser user, boolean admin) {
    validateReminderWindow(requestDto);
    Long ownerUserId = admin ? requireAdminOwnerFallback(user) : requireUserId(user);
    if (!admin) {
      validateManagedPatientProfile(ownerUserId, requestDto.getPatientProfileId());
    }
    validateProduct(requestDto.getProductId());

    TherapyReminder reminder = therapyReminderMapper.toEntity(requestDto);
    reminder.setOwnerUserId(ownerUserId);
    reminder.setDosageInstruction(
        TextSanitizer.sanitizeOptionalText(requestDto.getDosageInstruction()));
    reminder.setStatus(TherapyReminderStatus.ACTIVE);
    reminder.setNextReminderAt(calculateNextReminderAt(reminder));
    return therapyReminderMapper.toResponseDto(therapyReminderRepository.save(reminder));
  }

  @Transactional
  public TherapyReminderResponseDto updateReminder(Long id, TherapyReminderRequestDto requestDto) {
    TherapyReminder reminder = findReminderById(id);
    if (reminder.getStatus() == TherapyReminderStatus.COMPLETED
        || reminder.getStatus() == TherapyReminderStatus.CANCELED) {
      throw new BadRequestException("Completed or canceled reminders cannot be updated.");
    }

    therapyReminderMapper.updateEntity(requestDto, reminder);
    validateProduct(requestDto.getProductId());
    reminder.setDosageInstruction(
        TextSanitizer.sanitizeOptionalText(requestDto.getDosageInstruction()));
    reminder.setNextReminderAt(calculateNextReminderAt(reminder));
    if (reminder.getEndDate() != null && reminder.getEndDate().isBefore(LocalDate.now(clock))) {
      reminder.setStatus(TherapyReminderStatus.COMPLETED);
    }

    return therapyReminderMapper.toResponseDto(therapyReminderRepository.save(reminder));
  }

  @Transactional
  public TherapyReminderResponseDto updateReminder(
      Long id, TherapyReminderRequestDto requestDto, AuthenticatedUser user, boolean admin) {
    TherapyReminder reminder = findReminderById(id);
    validateReminderOwner(reminder, user, admin);
    if (!admin) {
      validateManagedPatientProfile(requireUserId(user), requestDto.getPatientProfileId());
    }
    validateProduct(requestDto.getProductId());
    if (reminder.getStatus() == TherapyReminderStatus.COMPLETED
        || reminder.getStatus() == TherapyReminderStatus.CANCELED) {
      throw new BadRequestException("Completed or canceled reminders cannot be updated.");
    }

    Long ownerUserId = reminder.getOwnerUserId();
    therapyReminderMapper.updateEntity(requestDto, reminder);
    reminder.setOwnerUserId(ownerUserId);
    reminder.setDosageInstruction(
        TextSanitizer.sanitizeOptionalText(requestDto.getDosageInstruction()));
    reminder.setNextReminderAt(calculateNextReminderAt(reminder));
    if (reminder.getEndDate() != null && reminder.getEndDate().isBefore(LocalDate.now(clock))) {
      reminder.setStatus(TherapyReminderStatus.COMPLETED);
    }

    return therapyReminderMapper.toResponseDto(therapyReminderRepository.save(reminder));
  }

  @Transactional
  public void deleteReminder(Long id) {
    therapyReminderRepository.delete(findReminderById(id));
  }

  @Transactional
  public void deleteReminder(Long id, AuthenticatedUser user, boolean admin) {
    TherapyReminder reminder = findReminderById(id);
    validateReminderOwner(reminder, user, admin);
    therapyReminderRepository.delete(reminder);
  }

  @Transactional
  public TherapyReminderResponseDto updateStatus(
      Long id, TherapyReminderStatusRequestDto requestDto, AuthenticatedUser user, boolean admin) {
    TherapyReminder reminder = findReminderById(id);
    validateReminderOwner(reminder, user, admin);
    TherapyReminderStatus targetStatus = requestDto.getStatus();
    if (targetStatus != TherapyReminderStatus.ACTIVE && targetStatus != TherapyReminderStatus.PAUSED) {
      throw new BadRequestException("Only ACTIVE and PAUSED statuses can be requested.");
    }
    if (reminder.getStatus() == TherapyReminderStatus.COMPLETED
        || reminder.getStatus() == TherapyReminderStatus.CANCELED) {
      throw new BadRequestException("Completed or canceled reminders cannot be reactivated.");
    }

    reminder.setStatus(targetStatus);
    if (targetStatus == TherapyReminderStatus.ACTIVE) {
      reminder.setNextReminderAt(calculateNextReminderAt(reminder, true));
    }
    return therapyReminderMapper.toResponseDto(therapyReminderRepository.save(reminder));
  }

  LocalDateTime calculateNextReminderAtAfterNow(TherapyReminder reminder) {
    return calculateNextReminderAt(reminder, true);
  }

  private TherapyReminder findReminderById(Long id) {
    return therapyReminderRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Therapy reminder not found with id: " + id));
  }

  private void validateReminderWindow(TherapyReminderRequestDto requestDto) {
    if (requestDto.getEndDate() != null && requestDto.getEndDate().isBefore(LocalDate.now(clock))) {
      throw new BadRequestException("endDate must not be in the past.");
    }
  }

  private LocalDateTime calculateNextReminderAt(TherapyReminder reminder) {
    return calculateNextReminderAt(reminder, false);
  }

  private LocalDateTime calculateNextReminderAt(TherapyReminder reminder, boolean requireAfterNow) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate candidateDate =
        reminder.getStartDate().isAfter(now.toLocalDate())
            ? reminder.getStartDate()
            : now.toLocalDate();

    while (reminder.getEndDate() == null || !candidateDate.isAfter(reminder.getEndDate())) {
      for (LocalTime slot : buildDailySchedule(reminder.getFrequencyPerDay())) {
        LocalDateTime candidate = LocalDateTime.of(candidateDate, slot);
        if (requireAfterNow ? candidate.isAfter(now) : !candidate.isBefore(now)) {
          return candidate;
        }
      }
      candidateDate = candidateDate.plusDays(1);
    }
    return null;
  }

  private List<LocalTime> buildDailySchedule(int frequencyPerDay) {
    if (frequencyPerDay == 1) {
      return List.of(DAILY_WINDOW_START);
    }

    int startMinute = DAILY_WINDOW_START.toSecondOfDay() / 60;
    int endMinute = DAILY_WINDOW_END.toSecondOfDay() / 60;
    int availableWindowMinutes = endMinute - startMinute;

    return java.util.stream.IntStream.range(0, frequencyPerDay)
        .mapToObj(
            index -> {
              long offsetMinutes =
                  Math.round((double) availableWindowMinutes * index / (frequencyPerDay - 1));
              return LocalTime.ofSecondOfDay((startMinute + offsetMinutes) * 60L);
            })
        .toList();
  }

  private void validateReminderOwner(
      TherapyReminder reminder, AuthenticatedUser user, boolean admin) {
    if (admin) {
      return;
    }
    Long ownerUserId = requireUserId(user);
    if (!Objects.equals(reminder.getOwnerUserId(), ownerUserId)) {
      throw new AccessDeniedException("You cannot manage this therapy reminder.");
    }
  }

  private void validateManagedPatientProfile(Long ownerUserId, Long patientProfileId) {
    UserHealthSnapshot currentUser =
        userHealthClient
            .getUser(ownerUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + ownerUserId));
    PatientHealthProfileSnapshot ownProfile = currentUser.getPatientProfile();
    if (ownProfile != null && Objects.equals(ownProfile.getId(), patientProfileId)) {
      return;
    }

    boolean ownsFamilyProfile =
        userHealthClient.getFamilyMembersByUserId(ownerUserId).stream()
            .map(FamilyMemberSnapshot::getPatientProfile)
            .filter(Objects::nonNull)
            .map(PatientHealthProfileSnapshot::getId)
            .anyMatch(patientProfileId::equals);
    if (!ownsFamilyProfile) {
      throw new AccessDeniedException("You cannot manage reminders for this patient profile.");
    }
  }

  private void validateProduct(Long productId) {
    var product =
        productCatalogClient
            .getProduct(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    if (Boolean.FALSE.equals(product.getIsActive())) {
      throw new BadRequestException("Inactive products cannot be used for reminders.");
    }
  }

  private Long requireUserId(AuthenticatedUser user) {
    if (user == null || user.userId() == null || user.userId() <= 0) {
      throw new AccessDeniedException("Authenticated user id is required.");
    }
    return user.userId();
  }

  private Long requireAdminOwnerFallback(AuthenticatedUser user) {
    return requireUserId(user);
  }
}

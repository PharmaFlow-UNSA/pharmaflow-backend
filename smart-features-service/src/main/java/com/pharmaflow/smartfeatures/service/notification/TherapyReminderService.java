package com.pharmaflow.smartfeatures.service.notification;

import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderResponseDto;
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.notification.TherapyReminderMapper;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TherapyReminderService {

  private static final LocalTime DAILY_WINDOW_START = LocalTime.of(8, 0);
  private static final LocalTime DAILY_WINDOW_END = LocalTime.of(22, 0);

  private final TherapyReminderRepository therapyReminderRepository;
  private final TherapyReminderMapper therapyReminderMapper;
  private final Clock clock;

  public TherapyReminderService(
      TherapyReminderRepository therapyReminderRepository,
      TherapyReminderMapper therapyReminderMapper,
      Clock clock) {
    this.therapyReminderRepository = therapyReminderRepository;
    this.therapyReminderMapper = therapyReminderMapper;
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
  public TherapyReminderResponseDto getReminder(Long id) {
    return therapyReminderMapper.toResponseDto(findReminderById(id));
  }

  @Transactional
  public TherapyReminderResponseDto createReminder(TherapyReminderRequestDto requestDto) {
    validateReminderWindow(requestDto);
    TherapyReminder reminder = therapyReminderMapper.toEntity(requestDto);
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
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate candidateDate =
        reminder.getStartDate().isAfter(now.toLocalDate())
            ? reminder.getStartDate()
            : now.toLocalDate();

    while (reminder.getEndDate() == null || !candidateDate.isAfter(reminder.getEndDate())) {
      for (LocalTime slot : buildDailySchedule(reminder.getFrequencyPerDay())) {
        LocalDateTime candidate = LocalDateTime.of(candidateDate, slot);
        if (!candidate.isBefore(now)) {
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
}

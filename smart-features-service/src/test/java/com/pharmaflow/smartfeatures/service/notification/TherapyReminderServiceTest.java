package com.pharmaflow.smartfeatures.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderResponseDto;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.mapper.notification.TherapyReminderMapper;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import com.pharmaflow.smartfeatures.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TherapyReminderServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2030-01-10T09:00:00Z"), ZoneOffset.UTC);

  @Mock private TherapyReminderRepository therapyReminderRepository;
  @Mock private UserHealthClient userHealthClient;
  @Mock private ProductCatalogClient productCatalogClient;

  private TherapyReminderService therapyReminderService;

  @BeforeEach
  void setUp() {
    therapyReminderService =
        new TherapyReminderService(
            therapyReminderRepository,
            new TherapyReminderMapper(new ModelMapperConfig().modelMapper()),
            userHealthClient,
            productCatalogClient,
            FIXED_CLOCK);
  }

  @Test
  void createReminderShouldSetActiveStatusAndNextReminder() {
    TherapyReminderRequestDto requestDto =
        new TherapyReminderRequestDto(
            10L, 20L, " Take once daily ", 2, LocalDate.of(2030, 1, 12), LocalDate.of(2030, 1, 17));
    ProductSnapshot product = new ProductSnapshot();
    product.setId(20L);
    product.setIsActive(true);
    when(userHealthClient.getUser(10L)).thenReturn(Optional.of(userWithPatientProfile(10L)));
    when(productCatalogClient.getProduct(20L)).thenReturn(Optional.of(product));
    when(therapyReminderRepository.save(any(TherapyReminder.class)))
        .thenAnswer(
            invocation -> {
              TherapyReminder reminder = invocation.getArgument(0);
              reminder.setReminderId(4L);
              return reminder;
            });

    TherapyReminderResponseDto response =
        therapyReminderService.createReminder(requestDto, new AuthenticatedUser("10", 10L), false);

    ArgumentCaptor<TherapyReminder> captor = ArgumentCaptor.forClass(TherapyReminder.class);
    verify(therapyReminderRepository).save(captor.capture());
    assertThat(captor.getValue().getOwnerUserId()).isEqualTo(10L);
    assertThat(captor.getValue().getDosageInstruction()).isEqualTo("Take once daily");
    assertThat(captor.getValue().getStatus()).isEqualTo(TherapyReminderStatus.ACTIVE);
    assertThat(captor.getValue().getNextReminderAt())
        .isEqualTo(requestDto.getStartDate().atTime(LocalTime.of(8, 0)));
    assertThat(response.getId()).isEqualTo(4L);
  }

  @Test
  void createReminderWithoutAuthenticatedUserShouldFailBeforeSaving() {
    TherapyReminderRequestDto requestDto =
        new TherapyReminderRequestDto(
            10L, 20L, "Take once daily", 1, LocalDate.of(2030, 1, 12), null);

    assertThatThrownBy(() -> therapyReminderService.createReminder(requestDto))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Authenticated user id is required.");

    verify(therapyReminderRepository, never()).save(any(TherapyReminder.class));
  }

  @Test
  void createReminderWithAdminMissingUserIdShouldFailBeforeSaving() {
    TherapyReminderRequestDto requestDto =
        new TherapyReminderRequestDto(
            10L, 20L, "Take once daily", 1, LocalDate.of(2030, 1, 12), null);

    assertThatThrownBy(
            () ->
                therapyReminderService.createReminder(
                    requestDto, new AuthenticatedUser("admin", null), true))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Authenticated user id is required.");

    verify(therapyReminderRepository, never()).save(any(TherapyReminder.class));
  }

  @Test
  void createReminderShouldRejectAlreadyEndedReminderWindow() {
    TherapyReminderRequestDto requestDto =
        new TherapyReminderRequestDto(
            10L, 20L, "Expired", 1, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 5));

    assertThatThrownBy(
            () ->
                therapyReminderService.createReminder(
                    requestDto, new AuthenticatedUser("10", 10L), false))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("endDate must not be in the past.");

    verify(therapyReminderRepository, never()).save(any(TherapyReminder.class));
  }

  @Test
  void updateReminderShouldMarkPastReminderCompleted() {
    TherapyReminder reminder =
        TherapyReminder.builder()
            .reminderId(9L)
            .patientProfileId(10L)
            .productId(20L)
            .status(TherapyReminderStatus.ACTIVE)
            .startDate(LocalDate.of(2030, 1, 1))
            .endDate(LocalDate.of(2030, 1, 5))
            .build();
    TherapyReminderRequestDto requestDto =
        new TherapyReminderRequestDto(
            10L, 20L, " Updated ", 1, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 9));
    ProductSnapshot product = new ProductSnapshot();
    product.setId(20L);
    product.setIsActive(true);

    when(therapyReminderRepository.findById(9L)).thenReturn(Optional.of(reminder));
    when(productCatalogClient.getProduct(20L)).thenReturn(Optional.of(product));
    when(therapyReminderRepository.save(reminder)).thenReturn(reminder);

    TherapyReminderResponseDto response = therapyReminderService.updateReminder(9L, requestDto);

    assertThat(reminder.getStatus()).isEqualTo(TherapyReminderStatus.COMPLETED);
    assertThat(reminder.getNextReminderAt()).isNull();
    assertThat(response.getStatus()).isEqualTo(TherapyReminderStatus.COMPLETED);
  }

  @Test
  void updateReminderShouldRejectCompletedReminder() {
    TherapyReminder reminder =
        TherapyReminder.builder().reminderId(10L).status(TherapyReminderStatus.COMPLETED).build();
    when(therapyReminderRepository.findById(10L)).thenReturn(Optional.of(reminder));

    assertThatThrownBy(
            () ->
                therapyReminderService.updateReminder(
                    10L,
                    new TherapyReminderRequestDto(
                        1L, 2L, null, 1, LocalDate.of(2030, 1, 10), null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Completed or canceled reminders cannot be updated.");

    verify(therapyReminderRepository, never()).save(any(TherapyReminder.class));
  }

  private UserHealthSnapshot userWithPatientProfile(Long patientProfileId) {
    PatientHealthProfileSnapshot patientProfile = new PatientHealthProfileSnapshot();
    patientProfile.setId(patientProfileId);
    UserHealthSnapshot user = new UserHealthSnapshot();
    user.setId(10L);
    user.setPatientProfile(patientProfile);
    return user;
  }
}

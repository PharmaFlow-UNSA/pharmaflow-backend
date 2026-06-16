package com.pharmaflow.smartfeatures.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.notification.NotificationDeliveryStatusRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationResponseDto;
import com.pharmaflow.smartfeatures.enums.notification.NotificationChannel;
import com.pharmaflow.smartfeatures.enums.notification.NotificationStatus;
import com.pharmaflow.smartfeatures.enums.notification.NotificationTriggerSource;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.mapper.notification.NotificationMapper;
import com.pharmaflow.smartfeatures.model.notification.Notification;
import com.pharmaflow.smartfeatures.model.notification.NotificationTrigger;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationRepository;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationTriggerRepository;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import com.pharmaflow.smartfeatures.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;

  @Mock private NotificationTriggerRepository notificationTriggerRepository;

  @Mock private TherapyReminderRepository therapyReminderRepository;

  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    notificationService =
        new NotificationService(
            notificationRepository,
            notificationTriggerRepository,
            therapyReminderRepository,
            new NotificationMapper(new ModelMapperConfig().modelMapper()));
  }

  @Test
  void createNotificationShouldDefaultToPendingAndCreateTherapyTrigger() {
    TherapyReminder reminder =
        TherapyReminder.builder()
            .reminderId(7L)
            .ownerUserId(10L)
            .patientProfileId(20L)
            .productId(30L)
            .build();
    NotificationRequestDto requestDto =
        new NotificationRequestDto(
            7L,
            10L,
            20L,
            NotificationType.THERAPY_REMINDER,
            NotificationChannel.IN_APP,
            " Reminder ",
            " Time ");

    when(therapyReminderRepository.findById(7L)).thenReturn(Optional.of(reminder));
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(
            invocation -> {
              Notification notification = invocation.getArgument(0);
              notification.setNotificationId(15L);
              return notification;
            });

    NotificationResponseDto response = notificationService.createNotification(requestDto);

    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(notificationCaptor.capture());
    assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.PENDING);
    assertThat(notificationCaptor.getValue().getTitle()).isEqualTo("Reminder");
    assertThat(notificationCaptor.getValue().getMessage()).isEqualTo("Time");
    assertThat(notificationCaptor.getValue().getSentAt()).isNull();

    ArgumentCaptor<NotificationTrigger> triggerCaptor =
        ArgumentCaptor.forClass(NotificationTrigger.class);
    verify(notificationTriggerRepository).save(triggerCaptor.capture());
    assertThat(triggerCaptor.getValue().getTriggerSource())
        .isEqualTo(NotificationTriggerSource.THERAPY);
    assertThat(triggerCaptor.getValue().getSourceEntityId()).isEqualTo(7L);
    assertThat(response.getId()).isEqualTo(15L);
    assertThat(response.getStatus()).isEqualTo(NotificationStatus.PENDING);
  }

  @Test
  void createNotificationShouldMapSystemNotificationToSystemTrigger() {
    NotificationRequestDto requestDto =
        new NotificationRequestDto(
            null,
            10L,
            null,
            NotificationType.SYSTEM,
            NotificationChannel.IN_APP,
            " System ",
            " Maintenance ");

    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(
            invocation -> {
              Notification notification = invocation.getArgument(0);
              notification.setNotificationId(16L);
              return notification;
            });

    notificationService.createNotification(requestDto);

    ArgumentCaptor<NotificationTrigger> triggerCaptor =
        ArgumentCaptor.forClass(NotificationTrigger.class);
    verify(notificationTriggerRepository).save(triggerCaptor.capture());
    assertThat(triggerCaptor.getValue().getTriggerSource())
        .isEqualTo(NotificationTriggerSource.SYSTEM);
    assertThat(triggerCaptor.getValue().getSourceEntityId()).isNull();
  }

  @Test
  void updateDeliveryStatusShouldRejectInvalidTransition() {
    Notification notification =
        Notification.builder().notificationId(3L).status(NotificationStatus.PENDING).build();
    when(notificationRepository.findById(3L)).thenReturn(Optional.of(notification));

    assertThatThrownBy(
            () ->
                notificationService.updateDeliveryStatus(
                    3L, new NotificationDeliveryStatusRequestDto(NotificationStatus.DELIVERED)))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid notification status transition from PENDING to DELIVERED.");

    verify(notificationRepository, never()).save(any(Notification.class));
  }

  @Test
  void getNotificationsShouldDenyNonAdminAccessToAnotherUser() {
    assertThatThrownBy(
            () ->
                notificationService.getNotifications(
                    11L, new AuthenticatedUser("10", 10L), false))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You do not have permission to access these notifications.");

    verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
  }

  @Test
  void getNotificationsShouldLimitNonAdminToAuthenticatedUser() {
    Notification notification =
        Notification.builder()
            .notificationId(1L)
            .userId(10L)
            .type(NotificationType.SYSTEM)
            .channel(NotificationChannel.IN_APP)
            .status(NotificationStatus.DELIVERED)
            .title("Notice")
            .message("Message")
            .createdAt(java.time.LocalDateTime.now())
            .build();
    when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L))
        .thenReturn(List.of(notification));

    List<NotificationResponseDto> notifications =
        notificationService.getNotifications(null, new AuthenticatedUser("10", 10L), false);

    assertThat(notifications).hasSize(1);
    verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(10L);
    verify(notificationRepository, never()).findAllByOrderByCreatedAtDesc();
  }

  @Test
  void markAsReadShouldDenyNonOwner() {
    Notification notification =
        Notification.builder()
            .notificationId(1L)
            .userId(10L)
            .status(NotificationStatus.DELIVERED)
            .build();
    when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

    assertThatThrownBy(
            () -> notificationService.markAsRead(1L, new AuthenticatedUser("11", 11L), false))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You do not have permission to access this notification.");

    verify(notificationRepository, never()).save(any(Notification.class));
  }
}

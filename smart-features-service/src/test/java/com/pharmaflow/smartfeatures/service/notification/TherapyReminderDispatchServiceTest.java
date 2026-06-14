package com.pharmaflow.smartfeatures.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import com.pharmaflow.smartfeatures.model.notification.Notification;
import com.pharmaflow.smartfeatures.model.notification.NotificationTrigger;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationRepository;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationTriggerRepository;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TherapyReminderDispatchServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2030-01-10T09:00:00Z"), ZoneOffset.UTC);

  @Mock private TherapyReminderRepository therapyReminderRepository;
  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationTriggerRepository notificationTriggerRepository;
  @Mock private TherapyReminderService therapyReminderService;
  @Mock private ProductCatalogClient productCatalogClient;
  @Mock private UserHealthClient userHealthClient;

  private TherapyReminderDispatchService dispatchService;

  @BeforeEach
  void setUp() {
    dispatchService =
        new TherapyReminderDispatchService(
            therapyReminderRepository,
            notificationRepository,
            notificationTriggerRepository,
            therapyReminderService,
            productCatalogClient,
            userHealthClient,
            FIXED_CLOCK);
  }

  @Test
  void dispatchDueRemindersShouldCancelReminderWithInvalidOwnerUserId() {
    TherapyReminder reminder =
        TherapyReminder.builder()
            .reminderId(11L)
            .ownerUserId(0L)
            .patientProfileId(10L)
            .productId(20L)
            .frequencyPerDay(1)
            .startDate(LocalDate.of(2030, 1, 10))
            .nextReminderAt(LocalDateTime.of(2030, 1, 10, 8, 0))
            .status(TherapyReminderStatus.ACTIVE)
            .build();
    when(
            therapyReminderRepository
                .findByStatusAndNextReminderAtLessThanEqualOrderByNextReminderAtAsc(
                    TherapyReminderStatus.ACTIVE, LocalDateTime.of(2030, 1, 10, 9, 0)))
        .thenReturn(List.of(reminder));

    dispatchService.dispatchDueReminders();

    ArgumentCaptor<TherapyReminder> captor = ArgumentCaptor.forClass(TherapyReminder.class);
    verify(therapyReminderRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(TherapyReminderStatus.CANCELED);
    assertThat(captor.getValue().getNextReminderAt()).isNull();
    verify(notificationRepository, never()).save(any(Notification.class));
    verify(notificationTriggerRepository, never()).save(any(NotificationTrigger.class));
  }
}

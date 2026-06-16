package com.pharmaflow.smartfeatures.service.notification;

import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.FamilyMemberSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.enums.notification.NotificationChannel;
import com.pharmaflow.smartfeatures.enums.notification.NotificationStatus;
import com.pharmaflow.smartfeatures.enums.notification.NotificationTriggerSource;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import com.pharmaflow.smartfeatures.model.notification.Notification;
import com.pharmaflow.smartfeatures.model.notification.NotificationTrigger;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationRepository;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationTriggerRepository;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TherapyReminderDispatchService {

  private static final Logger logger =
      LoggerFactory.getLogger(TherapyReminderDispatchService.class);

  private final TherapyReminderRepository therapyReminderRepository;
  private final NotificationRepository notificationRepository;
  private final NotificationTriggerRepository notificationTriggerRepository;
  private final TherapyReminderService therapyReminderService;
  private final ProductCatalogClient productCatalogClient;
  private final UserHealthClient userHealthClient;
  private final Clock clock;

  public TherapyReminderDispatchService(
      TherapyReminderRepository therapyReminderRepository,
      NotificationRepository notificationRepository,
      NotificationTriggerRepository notificationTriggerRepository,
      TherapyReminderService therapyReminderService,
      ProductCatalogClient productCatalogClient,
      UserHealthClient userHealthClient,
      Clock clock) {
    this.therapyReminderRepository = therapyReminderRepository;
    this.notificationRepository = notificationRepository;
    this.notificationTriggerRepository = notificationTriggerRepository;
    this.therapyReminderService = therapyReminderService;
    this.productCatalogClient = productCatalogClient;
    this.userHealthClient = userHealthClient;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${smartfeatures.reminders.dispatch-delay-ms:60000}")
  @Transactional
  public void dispatchDueReminders() {
    LocalDateTime now = LocalDateTime.now(clock);
    therapyReminderRepository
        .findByStatusAndNextReminderAtLessThanEqualOrderByNextReminderAtAsc(
            TherapyReminderStatus.ACTIVE, now)
        .forEach(reminder -> dispatchReminder(reminder, now));
  }

  private void dispatchReminder(TherapyReminder reminder, LocalDateTime now) {
    if (reminder.getOwnerUserId() == null || reminder.getOwnerUserId() <= 0) {
      logger.warn(
          "Canceling therapy reminder {} because owner user id is missing or invalid: {}",
          reminder.getReminderId(),
          reminder.getOwnerUserId());
      reminder.setStatus(TherapyReminderStatus.CANCELED);
      reminder.setNextReminderAt(null);
      therapyReminderRepository.save(reminder);
      return;
    }

    Notification notification =
        notificationRepository.save(
            Notification.builder()
                .therapyReminder(reminder)
                .userId(reminder.getOwnerUserId())
                .patientProfileId(reminder.getPatientProfileId())
                .type(NotificationType.THERAPY_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .title("Therapy reminder")
                .message(buildMessage(reminder))
                .status(NotificationStatus.DELIVERED)
                .createdAt(now)
                .sentAt(now)
                .build());

    notificationTriggerRepository.save(
        NotificationTrigger.builder()
            .notification(notification)
            .triggerSource(NotificationTriggerSource.THERAPY)
            .sourceEntityId(reminder.getReminderId())
            .triggeredAt(now)
            .build());

    LocalDateTime nextReminderAt = therapyReminderService.calculateNextReminderAtAfterNow(reminder);
    reminder.setNextReminderAt(nextReminderAt);
    if (nextReminderAt == null
        || (reminder.getEndDate() != null && reminder.getEndDate().isBefore(LocalDate.now(clock)))) {
      reminder.setStatus(TherapyReminderStatus.COMPLETED);
    }
    therapyReminderRepository.save(reminder);
  }

  private String buildMessage(TherapyReminder reminder) {
    String productName = productName(reminder);
    String profilePrefix = profilePrefix(reminder);
    String message = profilePrefix + productName + ".";
    if (reminder.getDosageInstruction() != null && !reminder.getDosageInstruction().isBlank()) {
      message += " " + reminder.getDosageInstruction();
    }
    return message;
  }

  private String productName(TherapyReminder reminder) {
    try {
      return productCatalogClient
          .getProduct(reminder.getProductId())
          .map(ProductSnapshot::getName)
          .filter(name -> !name.isBlank())
          .orElse("your medication");
    } catch (RuntimeException ignored) {
      return "your medication";
    }
  }

  private String profilePrefix(TherapyReminder reminder) {
    try {
      UserHealthSnapshot owner = userHealthClient.getUser(reminder.getOwnerUserId()).orElse(null);
      if (owner != null) {
        PatientHealthProfileSnapshot ownProfile = owner.getPatientProfile();
        if (ownProfile != null
            && Objects.equals(ownProfile.getId(), reminder.getPatientProfileId())) {
          return "Time to take ";
        }
      }

      String familyName =
          userHealthClient.getFamilyMembersByUserId(reminder.getOwnerUserId()).stream()
              .filter(member -> member.getPatientProfile() != null)
              .filter(
                  member ->
                      Objects.equals(
                          member.getPatientProfile().getId(), reminder.getPatientProfileId()))
              .map(FamilyMemberSnapshot::getFirstName)
              .filter(name -> name != null && !name.isBlank())
              .findFirst()
              .orElse("your family member");
      return "Time for " + familyName + "'s ";
    } catch (RuntimeException ignored) {
      return "Time to take ";
    }
  }
}

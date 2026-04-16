package com.pharmaflow.smartfeatures.service.notification;

import com.pharmaflow.smartfeatures.dto.notification.NotificationDeliveryStatusRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationResponseDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationTriggerResponseDto;
import com.pharmaflow.smartfeatures.enums.notification.NotificationStatus;
import com.pharmaflow.smartfeatures.enums.notification.NotificationTriggerSource;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.notification.NotificationMapper;
import com.pharmaflow.smartfeatures.model.notification.Notification;
import com.pharmaflow.smartfeatures.model.notification.NotificationTrigger;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationRepository;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationTriggerRepository;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTriggerRepository notificationTriggerRepository;
    private final TherapyReminderRepository therapyReminderRepository;
    private final NotificationMapper notificationMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationTriggerRepository notificationTriggerRepository,
            TherapyReminderRepository therapyReminderRepository,
            NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationTriggerRepository = notificationTriggerRepository;
        this.therapyReminderRepository = therapyReminderRepository;
        this.notificationMapper = notificationMapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long userId) {
        return (userId != null
                        ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                        : notificationRepository.findAllByOrderByCreatedAtDesc())
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponseDto getNotification(Long id) {
        return notificationMapper.toResponseDto(findNotificationById(id));
    }

    @Transactional
    public NotificationResponseDto createNotification(NotificationRequestDto requestDto) {
        Notification notification = notificationMapper.toEntity(requestDto);
        notification.setTitle(sanitizeRequired(requestDto.getTitle(), "title"));
        notification.setMessage(sanitizeRequired(requestDto.getMessage(), "message"));
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setSentAt(null);
        notification.setReadAt(null);
        notification.setTherapyReminder(findReminderOrNull(requestDto.getTherapyReminderId()));

        Notification savedNotification = notificationRepository.save(notification);
        createTrigger(savedNotification, requestDto.getType());
        return notificationMapper.toResponseDto(savedNotification);
    }

    @Transactional
    public NotificationResponseDto updateDeliveryStatus(Long id, NotificationDeliveryStatusRequestDto requestDto) {
        Notification notification = findNotificationById(id);
        NotificationStatus targetStatus = requestDto.getStatus();

        if (targetStatus == NotificationStatus.READ || targetStatus == NotificationStatus.PENDING) {
            throw new BadRequestException("Use mark-read for READ status and create endpoint for PENDING status.");
        }

        validateTransition(notification.getStatus(), targetStatus);
        notification.setStatus(targetStatus);
        if (targetStatus == NotificationStatus.SENT && notification.getSentAt() == null) {
            notification.setSentAt(LocalDateTime.now());
        }
        if (targetStatus == NotificationStatus.DELIVERED && notification.getSentAt() == null) {
            notification.setSentAt(LocalDateTime.now());
        }
        return notificationMapper.toResponseDto(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponseDto markAsRead(Long id) {
        Notification notification = findNotificationById(id);
        if (notification.getStatus() != NotificationStatus.DELIVERED && notification.getStatus() != NotificationStatus.READ) {
            throw new BadRequestException("Only delivered notifications can be marked as read.");
        }

        notification.setStatus(NotificationStatus.READ);
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return notificationMapper.toResponseDto(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationTriggerResponseDto> getTriggers(Long notificationId) {
        findNotificationById(notificationId);
        return notificationTriggerRepository.findByNotificationNotificationIdOrderByTriggeredAtDesc(notificationId).stream()
                .map(notificationMapper::toTriggerResponseDto)
                .toList();
    }

    private Notification findNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }

    private TherapyReminder findReminderOrNull(Long reminderId) {
        if (reminderId == null) {
            return null;
        }
        return therapyReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy reminder not found with id: " + reminderId));
    }

    private void createTrigger(Notification notification, NotificationType notificationType) {
        NotificationTriggerSource source = switch (notificationType) {
            case THERAPY_REMINDER -> NotificationTriggerSource.THERAPY;
            case REFILL_ALERT -> NotificationTriggerSource.AUTO_REFILL;
            case RECALL_ALERT -> NotificationTriggerSource.RECALL;
            case CHAT -> NotificationTriggerSource.CHATBOT;
            case SYSTEM -> NotificationTriggerSource.FRAUD;
        };

        NotificationTrigger trigger = NotificationTrigger.builder()
                .notification(notification)
                .triggerSource(source)
                .sourceEntityId(
                        notification.getTherapyReminder() != null
                                ? notification.getTherapyReminder().getReminderId()
                                : null)
                .triggeredAt(LocalDateTime.now())
                .build();
        notificationTriggerRepository.save(trigger);
    }

    private void validateTransition(NotificationStatus currentStatus, NotificationStatus targetStatus) {
        boolean valid = switch (currentStatus) {
            case PENDING -> targetStatus == NotificationStatus.SENT || targetStatus == NotificationStatus.FAILED;
            case SENT -> targetStatus == NotificationStatus.DELIVERED || targetStatus == NotificationStatus.FAILED;
            case DELIVERED -> targetStatus == NotificationStatus.READ;
            case READ, FAILED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "Invalid notification status transition from " + currentStatus + " to " + targetStatus + ".");
        }
    }

    private String sanitizeRequired(String value, String fieldName) {
        String sanitized = TextSanitizer.sanitizeRequiredText(value);
        if (sanitized == null) {
            throw new BadRequestException(fieldName + " is required.");
        }
        return sanitized;
    }
}

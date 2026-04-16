package com.pharmaflow.smartfeatures.dto.notification;

import com.pharmaflow.smartfeatures.enums.notification.NotificationChannel;
import com.pharmaflow.smartfeatures.enums.notification.NotificationStatus;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;
    private Long therapyReminderId;
    private Long userId;
    private Long patientProfileId;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationChannel channel;
    private NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}

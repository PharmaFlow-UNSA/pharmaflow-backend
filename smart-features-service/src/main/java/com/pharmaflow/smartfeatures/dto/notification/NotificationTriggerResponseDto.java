package com.pharmaflow.smartfeatures.dto.notification;

import com.pharmaflow.smartfeatures.enums.notification.NotificationTriggerSource;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTriggerResponseDto {

    private Long id;
    private Long notificationId;
    private NotificationTriggerSource triggerSource;
    private Long sourceEntityId;
    private LocalDateTime triggeredAt;
}

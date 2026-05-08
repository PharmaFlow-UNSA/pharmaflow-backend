package com.pharmaflow.smartfeatures.dto.notification;

import com.pharmaflow.smartfeatures.enums.notification.NotificationChannel;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {

  @NullablePositive(message = "therapyReminderId must be positive")
  private Long therapyReminderId;

  @NotNull(message = "userId is required")
  @Positive(message = "userId must be positive")
  private Long userId;

  @NullablePositive(message = "patientProfileId must be positive")
  private Long patientProfileId;

  @NotNull(message = "type is required")
  private NotificationType type;

  @NotNull(message = "channel is required")
  private NotificationChannel channel;

  @SanitizedTextSize(
      min = 3,
      max = 120,
      allowBlank = false,
      message = "title must be between 3 and 120 characters after trimming")
  private String title;

  @SanitizedTextSize(
      min = 3,
      max = 1000,
      allowBlank = false,
      message = "message must be between 3 and 1000 characters after trimming")
  private String message;
}

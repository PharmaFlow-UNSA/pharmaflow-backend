package com.pharmaflow.smartfeatures.dto.notification;

import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TherapyReminderStatusRequestDto {

  @NotNull(message = "status is required")
  private TherapyReminderStatus status;
}

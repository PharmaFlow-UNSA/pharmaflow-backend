package com.pharmaflow.smartfeatures.dto.notification;

import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TherapyReminderResponseDto {

  private Long id;
  private Long ownerUserId;
  private Long patientProfileId;
  private Long productId;
  private String dosageInstruction;
  private Integer frequencyPerDay;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDateTime nextReminderAt;
  private TherapyReminderStatus status;
}

package com.pharmaflow.smartfeatures.dto.notification;

import com.pharmaflow.smartfeatures.validation.ChronologicalRange;
import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ChronologicalRange(
    startField = "startDate",
    endField = "endDate",
    message = "endDate must not be before startDate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TherapyReminderRequestDto {

  @NotNull(message = "patientProfileId is required")
  @Positive(message = "patientProfileId must be positive")
  private Long patientProfileId;

  @NotNull(message = "productId is required")
  @Positive(message = "productId must be positive")
  private Long productId;

  @SanitizedTextSize(max = 500, message = "dosageInstruction must not exceed 500 characters")
  private String dosageInstruction;

  @NotNull(message = "frequencyPerDay is required")
  @Min(value = 1, message = "frequencyPerDay must be at least 1")
  @Max(value = 24, message = "frequencyPerDay must be at most 24")
  private Integer frequencyPerDay;

  @NotNull(message = "startDate is required")
  private LocalDate startDate;

  private LocalDate endDate;
}

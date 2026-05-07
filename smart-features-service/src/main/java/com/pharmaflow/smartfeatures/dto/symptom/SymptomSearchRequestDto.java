package com.pharmaflow.smartfeatures.dto.symptom;

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
public class SymptomSearchRequestDto {

  @NotNull(message = "userId is required")
  @Positive(message = "userId must be positive")
  private Long userId;

  @NullablePositive(message = "patientProfileId must be positive")
  private Long patientProfileId;

  @SanitizedTextSize(
      min = 2,
      max = 255,
      allowBlank = false,
      message = "searchQuery must be between 2 and 255 characters after trimming")
  private String searchQuery;
}

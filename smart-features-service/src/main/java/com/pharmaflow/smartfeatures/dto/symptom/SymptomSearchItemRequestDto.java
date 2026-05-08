package com.pharmaflow.smartfeatures.dto.symptom;

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
public class SymptomSearchItemRequestDto {

  @NotNull(message = "symptomId is required")
  @Positive(message = "symptomId must be positive")
  private Long symptomId;
}

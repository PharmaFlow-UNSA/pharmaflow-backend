package com.pharmaflow.smartfeatures.dto.symptom;

import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class SymptomProductMatchRequestDto {

  @NotNull(message = "productId is required")
  @Positive(message = "productId must be positive")
  private Long productId;

  @DecimalMin(value = "0.0", message = "relevanceScore must be at least 0.0")
  @DecimalMax(value = "1.0", message = "relevanceScore must be at most 1.0")
  private Double relevanceScore;

  @SanitizedTextSize(max = 500, message = "matchReason must not exceed 500 characters")
  private String matchReason;
}

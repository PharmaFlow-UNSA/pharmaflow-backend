package com.pharmaflow.smartfeatures.dto.fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleRequestDto {

  @SanitizedTextSize(
      min = 3,
      max = 100,
      allowBlank = false,
      message = "ruleName must be between 3 and 100 characters after trimming")
  private String ruleName;

  @SanitizedTextSize(
      min = 3,
      max = 80,
      allowBlank = false,
      message = "ruleCode must be between 3 and 80 characters after trimming")
  private String ruleCode;

  @SanitizedTextSize(
      min = 3,
      max = 50,
      allowBlank = false,
      message = "category must be between 3 and 50 characters after trimming")
  private String category;

  @SanitizedTextSize(max = 500, message = "description must not exceed 500 characters")
  private String description;

  @NotNull(message = "weight is required")
  @DecimalMin(value = "0.0", inclusive = false, message = "weight must be greater than 0")
  @DecimalMax(value = "100.0", message = "weight must be at most 100")
  private Double weight;

  @JsonProperty("isActive")
  @NotNull(message = "isActive is required")
  private Boolean active;
}

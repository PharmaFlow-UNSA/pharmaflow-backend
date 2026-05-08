package com.pharmaflow.smartfeatures.dto.fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleResponseDto {

  private Long id;
  private String ruleName;
  private String ruleCode;
  private String category;
  private String description;
  private Double weight;

  @JsonProperty("isActive")
  private Boolean active;
}

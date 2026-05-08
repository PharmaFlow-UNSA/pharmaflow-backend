package com.pharmaflow.smartfeatures.dto.symptom;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SymptomResponseDto {

  private Long id;
  private String name;
  private String description;
  private List<String> tags;
  private SymptomSeverityLevel severityLevel;

  @JsonProperty("isActive")
  private Boolean active;

  public SymptomResponseDto(
      Long id,
      String name,
      String description,
      SymptomSeverityLevel severityLevel,
      Boolean active) {
    this(id, name, description, List.of(), severityLevel, active);
  }
}

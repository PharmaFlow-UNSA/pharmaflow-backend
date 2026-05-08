package com.pharmaflow.smartfeatures.dto.symptom;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SymptomRequestDto {

  @NotBlank(message = "Symptom name is required")
  @SanitizedTextSize(
      min = 2,
      max = 100,
      allowBlank = false,
      message = "Symptom name must be between 2 and 100 characters after trimming")
  @Pattern(
      regexp = "^[\\p{L}\\p{N}\\s,.'()/-]+$",
      message =
          "Symptom name can only contain letters, numbers, spaces, commas, dots, apostrophes, parentheses, slashes and hyphens")
  private String name;

  @SanitizedTextSize(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  private List<String> tags;

  private SymptomSeverityLevel severityLevel;

  @JsonProperty("isActive")
  @NotNull(message = "isActive is required")
  private Boolean active;

  public SymptomRequestDto(
      String name, String description, SymptomSeverityLevel severityLevel, Boolean active) {
    this(name, description, List.of(), severityLevel, active);
  }
}

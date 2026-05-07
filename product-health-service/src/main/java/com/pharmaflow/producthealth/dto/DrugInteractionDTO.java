package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteractionDTO {

    private Long id;

    @NotNull(message = "Substance A ID is required")
    @Positive(message = "Substance A ID must be a positive number")
    private Long substanceAId;

    @NotNull(message = "Substance B ID is required")
    @Positive(message = "Substance B ID must be a positive number")
    private Long substanceBId;

    // Response only - for readability
    private String substanceAName;
    private String substanceBName;

    @NotBlank(message = "Severity level is required")
    @Pattern(regexp = "^(MINOR|MODERATE|MAJOR|CONTRAINDICATED)$",
             message = "Severity must be one of: MINOR, MODERATE, MAJOR, CONTRAINDICATED")
    private String severity;

    @NotBlank(message = "Interaction description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @Size(max = 2000, message = "Clinical recommendation must not exceed 2000 characters")
    private String clinicalRecommendation;
}

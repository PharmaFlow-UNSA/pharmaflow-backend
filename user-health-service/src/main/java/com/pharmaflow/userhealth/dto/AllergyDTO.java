package com.pharmaflow.userhealth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllergyDTO {

    private Long id;

    @NotBlank(message = "Allergen is required")
    @Size(min = 2, max = 100, message = "Allergen must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9À-ÿ\\s,.-]+$",
             message = "Allergen can only contain letters, numbers, spaces, commas, dots and hyphens")
    private String allergen;

    @Pattern(regexp = "^(Low|Moderate|High|Severe|Life-Threatening)?$",
             message = "Severity must be one of: Low, Moderate, High, Severe, Life-Threatening")
    @Size(max = 50, message = "Severity must not exceed 50 characters")
    private String severity;

    @Size(max = 100, message = "Active substance must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9À-ÿ\\s,.-]*$",
             message = "Active substance can only contain letters, numbers, spaces, commas, dots and hyphens")
    private String activeSubstance;
}


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
public class TherapyDTO {

    private Long id;

    @NotBlank(message = "Medication name is required")
    @Size(min = 2, max = 100, message = "Medication name must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9À-ÿ\\s,.-]+$",
             message = "Medication name can only contain letters, numbers, spaces, commas, dots and hyphens")
    private String medicationName;

    @Size(max = 50, message = "Dosage must not exceed 50 characters")
    @Pattern(regexp = "^[0-9.,\\s]*(mg|g|ml|mcg|IU|%)?$",
             message = "Dosage must be a valid format (e.g., 500mg, 5ml, 2.5g)")
    private String dosage;

    @Size(max = 50, message = "Frequency must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s/-]*(daily|weekly|monthly|hourly|times|per day)?$",
             message = "Frequency must be a valid format (e.g., 2 times daily, once weekly)")
    private String frequency;
}


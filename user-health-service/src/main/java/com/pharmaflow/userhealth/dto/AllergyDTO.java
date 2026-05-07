package com.pharmaflow.userhealth.dto;

import com.pharmaflow.userhealth.models.enums.Severity;
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

    private Severity severity;

    @Size(max = 100, message = "Active substance must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9À-ÿ\\s,.-]*$",
             message = "Active substance can only contain letters, numbers, spaces, commas, dots and hyphens")
    private String activeSubstance;
}


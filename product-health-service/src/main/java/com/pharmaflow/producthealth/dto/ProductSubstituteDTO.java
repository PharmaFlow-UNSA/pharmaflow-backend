package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSubstituteDTO {

    private Long id;

    @NotNull(message = "Original product ID is required")
    @Positive(message = "Original product ID must be a positive number")
    private Long originalProductId;

    @NotNull(message = "Substitute product ID is required")
    @Positive(message = "Substitute product ID must be a positive number")
    private Long substituteProductId;

    private String originalProductName;
    private String substituteProductName;

    @NotBlank(message = "Substitute type is required")
    @Pattern(regexp = "^(GENERIC|THERAPEUTIC|BIOSIMILAR)$",
             message = "Substitute type must be one of: GENERIC, THERAPEUTIC, BIOSIMILAR")
    private String substituteType;

    @NotNull(message = "Therapeutic equivalent flag is required")
    private Boolean isTherapeuticEquivalent;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}

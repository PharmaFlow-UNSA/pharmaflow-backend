package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraindicationDTO {

    private Long id;

    @NotNull(message = "ID supstance je obavezan")
    @Positive(message = "Substance ID must be a positive number")
    private Long substanceId;

    private String substanceName;

    @NotBlank(message = "Contraindication type is required")
    @Pattern(regexp = "^(DISEASE|ALLERGY|AGE|PREGNANCY|BREASTFEEDING|CONDITION)$",
             message = "Type must be one of: DISEASE, ALLERGY, AGE, PREGNANCY, BREASTFEEDING, CONDITION")
    private String type;

    @NotBlank(message = "Condition name is required")
    @Size(min = 2, max = 200, message = "Condition name must be between 2 and 200 characters")
    private String conditionName;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Severity type is required")
    @Pattern(regexp = "^(ABSOLUTE|RELATIVE)$",
             message = "Severity type must be one of: ABSOLUTE, RELATIVE")
    private String severityType;
}

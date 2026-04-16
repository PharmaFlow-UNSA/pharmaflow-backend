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
    @Positive(message = "ID supstance mora biti pozitivan broj")
    private Long substanceId;

    private String substanceName;

    @NotBlank(message = "Tip kontraindikacije je obavezan")
    @Pattern(regexp = "^(DISEASE|ALLERGY|AGE|PREGNANCY|BREASTFEEDING|CONDITION)$",
             message = "Tip mora biti jedan od: DISEASE, ALLERGY, AGE, PREGNANCY, BREASTFEEDING, CONDITION")
    private String type;

    @NotBlank(message = "Naziv stanja je obavezan")
    @Size(min = 2, max = 200, message = "Naziv stanja mora biti između 2 i 200 karaktera")
    private String conditionName;

    @Size(max = 2000, message = "Opis ne smije biti duži od 2000 karaktera")
    private String description;

    @NotBlank(message = "Tip ozbiljnosti je obavezan")
    @Pattern(regexp = "^(ABSOLUTE|RELATIVE)$",
             message = "Tip ozbiljnosti mora biti jedan od: ABSOLUTE, RELATIVE")
    private String severityType;
}

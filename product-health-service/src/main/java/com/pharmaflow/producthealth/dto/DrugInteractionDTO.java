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

    @NotNull(message = "ID supstance A je obavezan")
    @Positive(message = "ID supstance A mora biti pozitivan broj")
    private Long substanceAId;

    @NotNull(message = "ID supstance B je obavezan")
    @Positive(message = "ID supstance B mora biti pozitivan broj")
    private Long substanceBId;

    // Samo za response - radi čitljivosti
    private String substanceAName;
    private String substanceBName;

    @NotBlank(message = "Nivo ozbiljnosti je obavezan")
    @Pattern(regexp = "^(MINOR|MODERATE|MAJOR|CONTRAINDICATED)$",
             message = "Severity mora biti jedan od: MINOR, MODERATE, MAJOR, CONTRAINDICATED")
    private String severity;

    @NotBlank(message = "Opis interakcije je obavezan")
    @Size(min = 10, max = 2000, message = "Opis mora biti između 10 i 2000 karaktera")
    private String description;

    @Size(max = 2000, message = "Klinička preporuka ne smije biti duža od 2000 karaktera")
    private String clinicalRecommendation;
}

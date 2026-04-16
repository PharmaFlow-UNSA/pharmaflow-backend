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

    @NotNull(message = "ID originalnog proizvoda je obavezan")
    @Positive(message = "ID originalnog proizvoda mora biti pozitivan broj")
    private Long originalProductId;

    @NotNull(message = "ID zamjenskog proizvoda je obavezan")
    @Positive(message = "ID zamjenskog proizvoda mora biti pozitivan broj")
    private Long substituteProductId;

    private String originalProductName;
    private String substituteProductName;

    @NotBlank(message = "Tip zamjene je obavezan")
    @Pattern(regexp = "^(GENERIC|THERAPEUTIC|BIOSIMILAR)$",
             message = "Tip zamjene mora biti jedan od: GENERIC, THERAPEUTIC, BIOSIMILAR")
    private String substituteType;

    @NotNull(message = "Obavezno je naznačiti da li je terapijski ekvivalent")
    private Boolean isTherapeuticEquivalent;

    @Size(max = 500, message = "Napomena ne smije biti duža od 500 karaktera")
    private String note;
}

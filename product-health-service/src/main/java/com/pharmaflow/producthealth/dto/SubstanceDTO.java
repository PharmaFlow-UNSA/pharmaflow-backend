package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubstanceDTO {

    private Long id;

    @NotBlank(message = "INN (International Nonproprietary Name) je obavezan")
    @Size(min = 2, max = 100, message = "INN mora biti između 2 i 100 karaktera")
    private String inn;

    @Size(max = 150, message = "Uobičajeni naziv ne smije biti duži od 150 karaktera")
    private String commonName;

    @Size(max = 1000, message = "Opis ne smije biti duži od 1000 karaktera")
    private String description;

    @Pattern(regexp = "^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$|^$",
             message = "ATC kod mora biti u formatu: slovo + 2 broja + 2 slova + 2 broja (npr. N02BE01)")
    private String atcCode;
}

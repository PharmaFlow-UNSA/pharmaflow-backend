package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;

    @NotBlank(message = "Naziv kategorije je obavezan")
    @Size(min = 2, max = 100, message = "Naziv kategorije mora biti između 2 i 100 karaktera")
    private String name;

    @Size(max = 500, message = "Opis ne smije biti duži od 500 karaktera")
    private String description;

    private Long parentCategoryId;
}

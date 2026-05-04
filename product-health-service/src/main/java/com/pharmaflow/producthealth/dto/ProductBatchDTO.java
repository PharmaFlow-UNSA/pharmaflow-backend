package com.pharmaflow.producthealth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO za batch unos više proizvoda u jednom zahtjevu.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchDTO {

    @NotEmpty(message = "Lista proizvoda ne smije biti prazna")
    @Size(max = 50, message = "Maksimalan broj proizvoda u jednom batch unosu je 50")
    @Valid
    private List<ProductCreateDTO> products;
}

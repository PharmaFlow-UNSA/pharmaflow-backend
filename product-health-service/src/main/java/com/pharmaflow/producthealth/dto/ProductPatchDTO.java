package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO za parcijalno ažuriranje proizvoda (PATCH).
 * Sva polja su opcionalna — null znači "ne mijenjaj".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPatchDTO {

    @Size(min = 2, max = 200, message = "Naziv mora biti između 2 i 200 karaktera")
    private String name;

    @DecimalMin(value = "0.01", message = "Cijena mora biti veća od 0")
    @DecimalMax(value = "99999.99", message = "Cijena ne smije biti veća od 99999.99")
    private BigDecimal price;

    @Size(max = 2000, message = "Opis ne smije biti duži od 2000 karaktera")
    private String description;

    @Size(max = 150, message = "Naziv brenda ne smije biti duži od 150 karaktera")
    private String brandName;

    @Size(max = 100, message = "Veličina pakovanja ne smije biti duža od 100 karaktera")
    private String packageSize;

    @Size(max = 500, message = "Image URL ne smije biti duži od 500 karaktera")
    private String imageUrl;

    private Boolean requiresPrescription;

    @Pattern(regexp = "^(MEDICATION|SUPPLEMENT|COSMETIC|MEDICAL_DEVICE|OTHER)$",
             message = "Tip mora biti jedan od: MEDICATION, SUPPLEMENT, COSMETIC, MEDICAL_DEVICE, OTHER")
    private String productType;
}

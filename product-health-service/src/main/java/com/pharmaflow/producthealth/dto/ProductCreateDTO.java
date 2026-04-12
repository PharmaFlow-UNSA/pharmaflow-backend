package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateDTO {

    @NotBlank(message = "Naziv proizvoda je obavezan")
    @Size(min = 2, max = 200, message = "Naziv mora biti između 2 i 200 karaktera")
    private String name;

    @Pattern(regexp = "^[0-9]{8,14}$|^$",
             message = "Barkod mora sadržavati 8 do 14 cifara (EAN-8, EAN-13 ili GTIN-14)")
    private String barcode;

    @Size(max = 2000, message = "Opis ne smije biti duži od 2000 karaktera")
    private String description;

    @NotNull(message = "Cijena je obavezna")
    @DecimalMin(value = "0.01", message = "Cijena mora biti veća od 0")
    @DecimalMax(value = "99999.99", message = "Cijena ne smije biti veća od 99999.99")
    @Digits(integer = 5, fraction = 2, message = "Cijena može imati najviše 5 cifara i 2 decimale")
    private BigDecimal price;

    @Size(max = 150, message = "Naziv brenda ne smije biti duži od 150 karaktera")
    private String brandName;

    @NotBlank(message = "Proizvođač je obavezan")
    @Size(min = 2, max = 200, message = "Naziv proizvođača mora biti između 2 i 200 karaktera")
    private String manufacturer;

    @NotNull(message = "Obavezno je naznačiti da li je potreban recept")
    private Boolean requiresPrescription;

    @NotBlank(message = "Tip proizvoda je obavezan")
    @Pattern(regexp = "^(MEDICATION|SUPPLEMENT|COSMETIC|MEDICAL_DEVICE|OTHER)$",
             message = "Tip mora biti jedan od: MEDICATION, SUPPLEMENT, COSMETIC, MEDICAL_DEVICE, OTHER")
    private String productType;

    @Size(max = 500, message = "URL slike ne smije biti duži od 500 karaktera")
    private String imageUrl;

    @Size(max = 100, message = "Veličina pakovanja ne smije biti duža od 100 karaktera")
    private String packageSize;

    @NotNull(message = "ID kategorije je obavezan")
    @Positive(message = "ID kategorije mora biti pozitivan broj")
    private Long categoryId;

    private List<@Positive(message = "ID supstance mora biti pozitivan broj") Long> substanceIds;
}

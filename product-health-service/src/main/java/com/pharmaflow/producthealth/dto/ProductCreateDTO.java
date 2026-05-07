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

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @Pattern(regexp = "^[0-9]{8,14}$|^$",
             message = "Barcode must contain 8 to 14 digits (EAN-8, EAN-13 or GTIN-14)")
    private String barcode;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "99999.99", message = "Price must not exceed 99999.99")
    @Digits(integer = 5, fraction = 2, message = "Price can have at most 5 integer digits and 2 decimal places")
    private BigDecimal price;

    @Size(max = 150, message = "Brand name must not exceed 150 characters")
    private String brandName;

    @NotBlank(message = "Manufacturer is required")
    @Size(min = 2, max = 200, message = "Manufacturer name must be between 2 and 200 characters")
    private String manufacturer;

    @NotNull(message = "Prescription requirement flag is required")
    private Boolean requiresPrescription;

    @NotBlank(message = "Product type is required")
    @Pattern(regexp = "^(MEDICATION|SUPPLEMENT|COSMETIC|MEDICAL_DEVICE|OTHER)$",
             message = "Type must be one of: MEDICATION, SUPPLEMENT, COSMETIC, MEDICAL_DEVICE, OTHER")
    private String productType;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Size(max = 100, message = "Package size must not exceed 100 characters")
    private String packageSize;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be a positive number")
    private Long categoryId;

    private List<@Positive(message = "Substance ID must be a positive number") Long> substanceIds;
}

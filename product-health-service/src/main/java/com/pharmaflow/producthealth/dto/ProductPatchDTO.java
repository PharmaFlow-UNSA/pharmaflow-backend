package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for partial product update (PATCH).
 * All fields are optional — null means "do not change".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPatchDTO {

    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "99999.99", message = "Price must not exceed 99999.99")
    private BigDecimal price;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 150, message = "Brand name must not exceed 150 characters")
    private String brandName;

    @Size(max = 100, message = "Package size must not exceed 100 characters")
    private String packageSize;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private Boolean requiresPrescription;

    @Pattern(regexp = "^(MEDICATION|SUPPLEMENT|COSMETIC|MEDICAL_DEVICE|OTHER)$",
             message = "Type must be one of: MEDICATION, SUPPLEMENT, COSMETIC, MEDICAL_DEVICE, OTHER")
    private String productType;
}

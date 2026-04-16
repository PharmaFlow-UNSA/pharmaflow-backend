package com.pharmaflow.orderprescription.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoRefillSubscriptionDTO {

    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Dosage per day is required")
    @Min(value = 1, message = "Dosage per day must be at least 1")
    private Integer dosagePerDay;

    @NotNull(message = "Tablets per package is required")
    @Min(value = 1, message = "Tablets per package must be at least 1")
    private Integer tabletsPerPackage;

    private Integer intervalDays;

    private LocalDate nextOrderDate;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|PAUSED|CANCELLED)$",
            message = "Status must be one of: ACTIVE, PAUSED, CANCELLED")
    private String status;

    @NotBlank(message = "Shipping address is required")
    @Size(min = 5, max = 200, message = "Shipping address must be between 5 and 200 characters")
    private String shippingAddress;

    private Long prescriptionId;
}

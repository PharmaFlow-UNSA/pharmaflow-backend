package com.pharmaflow.pharmacyinventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {

    private Long id;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be at least 0")
    private Integer quantity;

    @Min(value = 0, message = "Reorder level must be at least 0")
    private Integer reorderLevel;

    private LocalDate lastRestocked;

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;
}

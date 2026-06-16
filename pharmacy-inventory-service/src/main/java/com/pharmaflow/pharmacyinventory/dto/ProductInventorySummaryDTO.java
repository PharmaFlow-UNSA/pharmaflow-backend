package com.pharmaflow.pharmacyinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventorySummaryDTO {

    private Long productId;

    private Integer totalQuantity;

    private Long pharmacyCount;

    private Boolean inStock;

    private Integer lowestQuantity;

    private Boolean lowStock;
}

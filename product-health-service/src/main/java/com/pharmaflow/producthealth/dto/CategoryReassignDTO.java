package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for transactional reassignment of multiple products to another category.
 * Used in service that calls multiple repository methods in a single transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryReassignDTO {

    @NotEmpty(message = "Product ID list must not be empty")
    private List<@Positive(message = "Product ID must be a positive number") Long> productIds;

    @NotNull(message = "Target category ID is required")
    @Positive(message = "Category ID must be a positive number")
    private Long targetCategoryId;
}

package com.pharmaflow.producthealth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for batch insert of multiple products in a single request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchDTO {

    @NotEmpty(message = "Product list must not be empty")
    @Size(max = 50, message = "Maximum number of products in a single batch is 50")
    @Valid
    private List<ProductCreateDTO> products;
}

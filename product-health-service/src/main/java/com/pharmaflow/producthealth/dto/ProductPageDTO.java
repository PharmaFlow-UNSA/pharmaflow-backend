package com.pharmaflow.producthealth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated response — contains data and page metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageDTO {

    private List<ProductDTO> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}

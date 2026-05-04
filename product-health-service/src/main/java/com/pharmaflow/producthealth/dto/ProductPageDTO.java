package com.pharmaflow.producthealth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO za paginiran odgovor — sadrži podatke i metapodatke o stranici.
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

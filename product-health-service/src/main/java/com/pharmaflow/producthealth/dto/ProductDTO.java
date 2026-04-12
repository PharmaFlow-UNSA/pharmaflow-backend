package com.pharmaflow.producthealth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;
    private String name;
    private String barcode;
    private String description;
    private BigDecimal price;
    private String brandName;
    private String manufacturer;
    private Boolean requiresPrescription;
    private String productType;
    private String imageUrl;
    private Boolean isActive;
    private String packageSize;
    private CategoryDTO category;
    private List<SubstanceDTO> substances;
}

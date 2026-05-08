package com.pharmaflow.smartfeatures.dto.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductSnapshot {

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
  private CategorySnapshot category;
  private List<SubstanceSnapshot> substances = new ArrayList<>();
}

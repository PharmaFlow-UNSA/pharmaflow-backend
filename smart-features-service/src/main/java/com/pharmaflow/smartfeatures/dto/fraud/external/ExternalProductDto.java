package com.pharmaflow.smartfeatures.dto.fraud.external;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalProductDto {

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
  private ExternalCategoryDto category;
  private List<ExternalSubstanceDto> substances = new ArrayList<>();
}

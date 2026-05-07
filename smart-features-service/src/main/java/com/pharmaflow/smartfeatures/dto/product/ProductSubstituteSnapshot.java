package com.pharmaflow.smartfeatures.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductSubstituteSnapshot {

  private Long id;
  private Long originalProductId;
  private Long substituteProductId;
  private String originalProductName;
  private String substituteProductName;
  private String substituteType;
  private Boolean isTherapeuticEquivalent;
  private String note;
}

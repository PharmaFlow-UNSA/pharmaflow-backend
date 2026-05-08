package com.pharmaflow.smartfeatures.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategorySnapshot {

  private Long id;
  private String name;
  private String description;
  private Long parentCategoryId;
}

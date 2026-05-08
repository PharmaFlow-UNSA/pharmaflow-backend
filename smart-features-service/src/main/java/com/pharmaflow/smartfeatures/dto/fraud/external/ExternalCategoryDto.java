package com.pharmaflow.smartfeatures.dto.fraud.external;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalCategoryDto {

  private Long id;
  private String name;
  private String description;
  private Long parentCategoryId;
}

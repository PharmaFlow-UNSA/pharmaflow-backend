package com.pharmaflow.smartfeatures.dto.userhealth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AllergySnapshot {

  private Long id;
  private String allergen;
  private String severity;
  private String activeSubstance;
}

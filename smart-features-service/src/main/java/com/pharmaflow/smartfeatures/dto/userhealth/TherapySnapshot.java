package com.pharmaflow.smartfeatures.dto.userhealth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TherapySnapshot {

  private Long id;
  private String medicationName;
  private String dosage;
  private String frequency;
}

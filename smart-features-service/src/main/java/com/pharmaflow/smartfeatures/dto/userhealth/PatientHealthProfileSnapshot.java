package com.pharmaflow.smartfeatures.dto.userhealth;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatientHealthProfileSnapshot {

  private Long id;
  private Double weight;
  private Double height;
  private String bloodType;
  private List<AllergySnapshot> allergies = new ArrayList<>();
  private List<TherapySnapshot> therapies = new ArrayList<>();
}

package com.pharmaflow.smartfeatures.dto.userhealth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FamilyMemberSnapshot {

  private Long id;
  private String firstName;
  private String relationship;
  private PatientHealthProfileSnapshot patientProfile;
  private Long userId;
}

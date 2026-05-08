package com.pharmaflow.smartfeatures.dto.userhealth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserHealthSnapshot {

  private Long id;
  private String firstName;
  private String lastName;
  private String email;
  private PatientHealthProfileSnapshot patientProfile;
}

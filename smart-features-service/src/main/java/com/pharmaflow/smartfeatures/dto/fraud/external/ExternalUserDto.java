package com.pharmaflow.smartfeatures.dto.fraud.external;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalUserDto {

  private Long id;
  private String firstName;
  private String lastName;
  private String email;
}

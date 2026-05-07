package com.pharmaflow.smartfeatures.dto.fraud.external;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalSubstanceDto {

  private Long id;
  private String inn;
  private String commonName;
  private String atcCode;
  private String description;
}

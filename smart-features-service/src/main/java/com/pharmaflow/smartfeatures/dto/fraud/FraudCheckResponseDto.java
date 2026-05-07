package com.pharmaflow.smartfeatures.dto.fraud;

import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponseDto {

  private Long id;
  private Long userId;
  private Long orderId;
  private Double riskScore;
  private FraudDecision decision;
  private LocalDateTime checkedAt;
}

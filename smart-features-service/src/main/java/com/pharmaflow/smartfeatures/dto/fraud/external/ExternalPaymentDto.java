package com.pharmaflow.smartfeatures.dto.fraud.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalPaymentDto {

  private Long id;
  private BigDecimal amount;
  private String method;
  private String status;
  private String transactionId;
  private LocalDateTime paidAt;
}

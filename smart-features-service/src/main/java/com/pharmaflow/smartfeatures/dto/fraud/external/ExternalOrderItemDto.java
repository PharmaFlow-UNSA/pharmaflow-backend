package com.pharmaflow.smartfeatures.dto.fraud.external;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalOrderItemDto {

  private Long id;
  private Long productId;
  private String productName;
  private Integer quantity;
  private BigDecimal unitPrice;
  private Long orderId;
}

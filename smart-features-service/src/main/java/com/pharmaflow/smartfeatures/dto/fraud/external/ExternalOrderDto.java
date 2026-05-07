package com.pharmaflow.smartfeatures.dto.fraud.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalOrderDto {

  private Long id;
  private Long userId;
  private String status;
  private BigDecimal totalAmount;
  private String shippingAddress;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<ExternalOrderItemDto> orderItems = new ArrayList<>();
  private ExternalPaymentDto payment;
  private Long prescriptionId;
}

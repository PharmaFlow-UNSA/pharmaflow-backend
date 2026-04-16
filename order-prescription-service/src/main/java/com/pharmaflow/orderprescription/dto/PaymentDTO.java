package com.pharmaflow.orderprescription.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private Long id;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(CARD|CASH|TRANSFER)$",
            message = "Payment method must be one of: CARD, CASH, TRANSFER")
    private String method;

    @NotBlank(message = "Payment status is required")
    @Pattern(regexp = "^(PENDING|COMPLETED|FAILED|REFUNDED)$",
            message = "Payment status must be one of: PENDING, COMPLETED, FAILED, REFUNDED")
    private String status;

    @Size(max = 100, message = "Transaction ID must not exceed 100 characters")
    private String transactionId;

    private LocalDateTime paidAt;
}

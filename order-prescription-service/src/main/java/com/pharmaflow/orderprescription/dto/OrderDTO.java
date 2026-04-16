package com.pharmaflow.orderprescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED)$",
            message = "Status must be one of: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED")
    private String status;

    private BigDecimal totalAmount;

    @NotBlank(message = "Shipping address is required")
    @Size(min = 5, max = 200, message = "Shipping address must be between 5 and 200 characters")
    private String shippingAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Valid
    private List<OrderItemDTO> orderItems = new ArrayList<>();

    @Valid
    private PaymentDTO payment;

    private Long prescriptionId;
}

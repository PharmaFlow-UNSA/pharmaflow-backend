package com.pharmaflow.orderprescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Shipping address is required")
    @Size(min = 5, max = 200, message = "Shipping address must be between 5 and 200 characters")
    private String shippingAddress;

    private Long prescriptionId;

    @Valid
    private List<OrderItemDTO> orderItems = new ArrayList<>();

    @Valid
    private PaymentDTO payment;
}

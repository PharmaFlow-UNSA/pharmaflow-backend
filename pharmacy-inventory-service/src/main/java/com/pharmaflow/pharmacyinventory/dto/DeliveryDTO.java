package com.pharmaflow.pharmacyinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDTO {

    private Long id;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Delivery address is required")
    @Size(min = 5, max = 200, message = "Delivery address must be between 5 and 200 characters")
    private String deliveryAddress;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PREPARING|IN_TRANSIT|DELIVERED|FAILED|RETURNED)$",
            message = "Status must be one of: PREPARING, IN_TRANSIT, DELIVERED, FAILED, RETURNED")
    private String status;

    private LocalDateTime estimatedDelivery;

    private LocalDateTime actualDelivery;

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;
}

package com.pharmaflow.pharmacyinventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|READY|CANCELLED|EXPIRED|COMPLETED)$",
            message = "Status must be one of: PENDING, READY, CANCELLED, EXPIRED, COMPLETED")
    private String status;

    @NotNull(message = "Reservation date is required")
    private LocalDateTime reservedAt;

    private LocalDateTime expiresAt;

    private String sagaCorrelationId;

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;
}

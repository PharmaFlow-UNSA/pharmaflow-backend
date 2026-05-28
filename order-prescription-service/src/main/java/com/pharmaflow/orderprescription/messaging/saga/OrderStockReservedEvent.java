package com.pharmaflow.orderprescription.messaging.saga;

import java.time.LocalDateTime;

/**
 * Reply event from pharmacy-inventory-service indicating that stock for every
 * item of the order was successfully deducted. Marks the saga as ready to
 * transition to its final CONFIRMED state.
 */
public record OrderStockReservedEvent(
        String correlationId,
        Long orderId,
        LocalDateTime reservedAt) {
}

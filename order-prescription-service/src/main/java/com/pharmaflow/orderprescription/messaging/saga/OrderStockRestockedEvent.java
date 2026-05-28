package com.pharmaflow.orderprescription.messaging.saga;

import java.time.LocalDateTime;

/**
 * Reply event from pharmacy-inventory-service confirming that the previously
 * deducted stock has been restored after an {@link OrderCancelledEvent}.
 */
public record OrderStockRestockedEvent(
        String correlationId,
        Long orderId,
        LocalDateTime restockedAt) {
}

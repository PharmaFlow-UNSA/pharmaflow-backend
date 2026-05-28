package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;

/**
 * Published on {@code order.stock.restocked} after handling
 * {@link OrderCancelledEvent} and restoring inventory levels.
 */
public record OrderStockRestockedEvent(
        String correlationId,
        Long orderId,
        LocalDateTime restockedAt) {
}

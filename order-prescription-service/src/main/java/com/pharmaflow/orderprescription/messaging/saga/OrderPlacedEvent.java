package com.pharmaflow.orderprescription.messaging.saga;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Emitted when an order is created and the order-fulfillment saga starts.
 * Consumed by pharmacy-inventory-service.
 */
public record OrderPlacedEvent(
        String correlationId,
        Long orderId,
        Long userId,
        Long pharmacyId,
        List<OrderPlacedItem> items,
        LocalDateTime placedAt) {
}

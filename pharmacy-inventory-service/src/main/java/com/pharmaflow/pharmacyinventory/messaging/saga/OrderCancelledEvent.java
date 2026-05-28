package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;

/**
 * Consumed from {@code order.cancelled}. Tells pharmacy-inventory-service to
 * restock the items that were previously deducted for the given correlation
 * id (i.e. the inverse of a successful {@link OrderPlacedEvent}).
 */
public record OrderCancelledEvent(
        String correlationId,
        Long orderId,
        String reason,
        LocalDateTime cancelledAt) {
}

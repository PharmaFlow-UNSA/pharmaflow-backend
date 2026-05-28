package com.pharmaflow.orderprescription.messaging.saga;

import java.time.LocalDateTime;

/**
 * Compensation event published when the order is cancelled after stock was
 * already reserved (or when the saga rolls back). Consumed by
 * pharmacy-inventory-service which restocks the affected products.
 */
public record OrderCancelledEvent(
        String correlationId,
        Long orderId,
        String reason,
        LocalDateTime cancelledAt) {
}

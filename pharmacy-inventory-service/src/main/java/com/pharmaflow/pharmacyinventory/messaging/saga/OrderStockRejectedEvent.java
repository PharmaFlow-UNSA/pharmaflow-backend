package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;

/**
 * Published on {@code order.stock.rejected} when stock could not be deducted
 * (e.g. insufficient inventory). Triggers the inverse action on the order side.
 */
public record OrderStockRejectedEvent(
        String correlationId,
        Long orderId,
        String reason,
        LocalDateTime rejectedAt) {
}

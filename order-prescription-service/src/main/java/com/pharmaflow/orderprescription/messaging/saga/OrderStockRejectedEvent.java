package com.pharmaflow.orderprescription.messaging.saga;

import java.time.LocalDateTime;

/**
 * Reply event from pharmacy-inventory-service indicating that stock could not
 * be reserved (e.g. insufficient inventory). Triggers the inverse action on
 * the order side (cancel order + mark saga COMPENSATED).
 */
public record OrderStockRejectedEvent(
        String correlationId,
        Long orderId,
        String reason,
        LocalDateTime rejectedAt) {
}

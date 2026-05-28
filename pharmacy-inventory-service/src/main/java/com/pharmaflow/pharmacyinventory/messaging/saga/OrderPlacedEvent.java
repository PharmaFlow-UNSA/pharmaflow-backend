package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consumed from {@code order.placed}. Carries the items the order service
 * wants stock deducted for and the pharmacy the order is being shipped from.
 */
public record OrderPlacedEvent(
        String correlationId,
        Long orderId,
        Long userId,
        Long pharmacyId,
        List<OrderPlacedItem> items,
        LocalDateTime placedAt) {
}

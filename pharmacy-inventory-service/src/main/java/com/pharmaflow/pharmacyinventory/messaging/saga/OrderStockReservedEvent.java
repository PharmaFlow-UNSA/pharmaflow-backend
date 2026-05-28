package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;

/**
 * Published on {@code order.stock.reserved} when every item has been
 * successfully deducted from inventory.
 */
public record OrderStockReservedEvent(
        String correlationId,
        Long orderId,
        LocalDateTime reservedAt) {
}

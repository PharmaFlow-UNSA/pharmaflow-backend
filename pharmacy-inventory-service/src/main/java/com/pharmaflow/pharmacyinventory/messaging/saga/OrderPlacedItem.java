package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.math.BigDecimal;

/**
 * Single line item carried inside {@link OrderPlacedEvent}. Mirror of the
 * record on the order-prescription side; both sides use plain records so
 * Jackson can deserialize by field name without sharing an entity type.
 */
public record OrderPlacedItem(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice) {
}

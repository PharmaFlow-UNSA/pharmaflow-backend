package com.pharmaflow.orderprescription.messaging.saga;

import java.math.BigDecimal;

/**
 * Single line item carried inside {@link OrderPlacedEvent}. Kept as a plain
 * record so Jackson can serialize/deserialize it across services without
 * sharing entity types.
 */
public record OrderPlacedItem(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice) {
}

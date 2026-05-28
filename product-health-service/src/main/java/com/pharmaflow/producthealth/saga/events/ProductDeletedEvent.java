package com.pharmaflow.producthealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event emitted when a product is deleted as a compensating action.
 * Signals pharmacy-inventory-service to also clean up any partial stock entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductDeletedEvent extends SagaEvent {

    private Long productId;
    private String reason; // COMPENSATION

    public ProductDeletedEvent(String sagaId, Long productId, String reason) {
        this.setSagaId(sagaId);
        this.setEventType("PRODUCT_DELETED");
        this.setSourceService("product-health-service");
        this.productId = productId;
        this.reason = reason;
    }
}

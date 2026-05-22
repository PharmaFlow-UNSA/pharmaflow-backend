package com.pharmaflow.producthealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event received when Pharmacy Inventory Service fails to create a stock entry.
 * Triggers compensating transaction: product deletion in product-health-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductStockCreationFailedEvent extends SagaEvent {

    private Long productId;
    private String reason;
    private String errorDetails;

    public ProductStockCreationFailedEvent(String sagaId, Long productId,
                                           String reason, String errorDetails) {
        this.setSagaId(sagaId);
        this.setEventType("PRODUCT_STOCK_CREATION_FAILED");
        this.setSourceService("pharmacy-inventory-service");
        this.productId = productId;
        this.reason = reason;
        this.errorDetails = errorDetails;
    }
}

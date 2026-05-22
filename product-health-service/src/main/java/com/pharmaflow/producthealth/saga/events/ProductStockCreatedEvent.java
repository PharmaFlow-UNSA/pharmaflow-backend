package com.pharmaflow.producthealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event received when Pharmacy Inventory Service successfully creates a stock entry.
 * Marks the product creation saga as COMPLETED.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductStockCreatedEvent extends SagaEvent {

    private Long productId;
    private Long stockEntryId;
    private String status; // SUCCESS

    public ProductStockCreatedEvent(String sagaId, Long productId, Long stockEntryId) {
        this.setSagaId(sagaId);
        this.setEventType("PRODUCT_STOCK_CREATED");
        this.setSourceService("pharmacy-inventory-service");
        this.productId = productId;
        this.stockEntryId = stockEntryId;
        this.status = "SUCCESS";
    }
}

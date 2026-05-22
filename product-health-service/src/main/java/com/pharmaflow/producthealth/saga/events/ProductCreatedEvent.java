package com.pharmaflow.producthealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event emitted when a new product is created in product-health-service.
 * Triggers stock entry creation in Pharmacy Inventory Service (Local Transaction 2).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductCreatedEvent extends SagaEvent {

    private Long productId;
    private String productName;
    private String barcode;
    private String manufacturer;
    private BigDecimal price;
    private String productType;
    private Integer initialStockQuantity;

    public ProductCreatedEvent(String sagaId, Long productId, String productName,
                               String barcode, String manufacturer, BigDecimal price,
                               String productType) {
        this.setSagaId(sagaId);
        this.setEventType("PRODUCT_CREATED");
        this.setSourceService("product-health-service");
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.manufacturer = manufacturer;
        this.price = price;
        this.productType = productType;
        this.initialStockQuantity = 0;
    }
}

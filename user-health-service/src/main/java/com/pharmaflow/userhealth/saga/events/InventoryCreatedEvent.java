package com.pharmaflow.userhealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event received when inventory account is successfully created in Pharmacy Service.
 * Marks the saga as completed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryCreatedEvent extends SagaEvent {

    private Long userId;
    private Long inventoryAccountId;
    private String status; // SUCCESS

    public InventoryCreatedEvent(String sagaId, Long userId, Long inventoryAccountId) {
        this.setSagaId(sagaId);
        this.setEventType("INVENTORY_CREATED");
        this.setSourceService("pharmacy-inventory-service");
        this.userId = userId;
        this.inventoryAccountId = inventoryAccountId;
        this.status = "SUCCESS";
    }
}


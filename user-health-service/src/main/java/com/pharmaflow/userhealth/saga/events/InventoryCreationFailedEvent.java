package com.pharmaflow.userhealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event received when inventory account creation fails.
 * Triggers compensating transaction (user deletion).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryCreationFailedEvent extends SagaEvent {

    private Long userId;
    private String reason;
    private String errorDetails;

    public InventoryCreationFailedEvent(String sagaId, Long userId, String reason, String errorDetails) {
        this.setSagaId(sagaId);
        this.setEventType("INVENTORY_CREATION_FAILED");
        this.setSourceService("pharmacy-inventory-service");
        this.userId = userId;
        this.reason = reason;
        this.errorDetails = errorDetails;
    }
}


package com.pharmaflow.userhealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event emitted when user is deleted as compensating action.
 * Completes the rollback process.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDeletedEvent extends SagaEvent {

    private Long userId;
    private String reason; // COMPENSATION

    public UserDeletedEvent(String sagaId, Long userId, String reason) {
        this.setSagaId(sagaId);
        this.setEventType("USER_DELETED");
        this.setSourceService("user-health-service");
        this.userId = userId;
        this.reason = reason;
    }
}


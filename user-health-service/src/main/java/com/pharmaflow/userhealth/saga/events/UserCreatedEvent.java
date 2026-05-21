package com.pharmaflow.userhealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event emitted when a new user is created.
 * Triggers inventory account creation in Pharmacy Inventory Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserCreatedEvent extends SagaEvent {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    public UserCreatedEvent(String sagaId, Long userId, String firstName, String lastName, String email, String role) {
        this.setSagaId(sagaId);
        this.setEventType("USER_CREATED");
        this.setSourceService("user-health-service");
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }
}


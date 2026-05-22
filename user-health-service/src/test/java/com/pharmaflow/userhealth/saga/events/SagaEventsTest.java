package com.pharmaflow.userhealth.saga.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Saga Event models.
 */
@DisplayName("Saga Events - Unit Tests")
class SagaEventsTest {

    @Test
    @DisplayName("UserCreatedEvent should contain all required fields")
    void userCreatedEvent_shouldContainAllFields() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;

        // When
        UserCreatedEvent event = new UserCreatedEvent(
                sagaId, userId, "John", "Doe", "john@test.com", "ROLE_USER"
        );

        // Then
        assertThat(event.getSagaId()).isEqualTo(sagaId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getFirstName()).isEqualTo("John");
        assertThat(event.getLastName()).isEqualTo("Doe");
        assertThat(event.getEmail()).isEqualTo("john@test.com");
        assertThat(event.getRole()).isEqualTo("ROLE_USER");
        assertThat(event.getEventType()).isEqualTo("USER_CREATED");
        assertThat(event.getSourceService()).isEqualTo("user-health-service");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("InventoryCreatedEvent should indicate success")
    void inventoryCreatedEvent_shouldIndicateSuccess() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;
        Long inventoryId = 42L;

        // When
        InventoryCreatedEvent event = new InventoryCreatedEvent(sagaId, userId, inventoryId);

        // Then
        assertThat(event.getSagaId()).isEqualTo(sagaId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getInventoryAccountId()).isEqualTo(inventoryId);
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getEventType()).isEqualTo("INVENTORY_CREATED");
        assertThat(event.getSourceService()).isEqualTo("pharmacy-inventory-service");
    }

    @Test
    @DisplayName("InventoryCreationFailedEvent should contain error details")
    void inventoryCreationFailedEvent_shouldContainErrorDetails() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;
        String reason = "Database error";
        String errorDetails = "Connection timeout after 30s";

        // When
        InventoryCreationFailedEvent event = new InventoryCreationFailedEvent(
                sagaId, userId, reason, errorDetails
        );

        // Then
        assertThat(event.getSagaId()).isEqualTo(sagaId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getReason()).isEqualTo(reason);
        assertThat(event.getErrorDetails()).isEqualTo(errorDetails);
        assertThat(event.getEventType()).isEqualTo("INVENTORY_CREATION_FAILED");
    }

    @Test
    @DisplayName("UserDeletedEvent should indicate compensation")
    void userDeletedEvent_shouldIndicateCompensation() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;

        // When
        UserDeletedEvent event = new UserDeletedEvent(sagaId, userId, "COMPENSATION");

        // Then
        assertThat(event.getSagaId()).isEqualTo(sagaId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getReason()).isEqualTo("COMPENSATION");
        assertThat(event.getEventType()).isEqualTo("USER_DELETED");
    }

    @Test
    @DisplayName("Each event should have unique eventId")
    void events_shouldHaveUniqueEventIds() {
        // When
        UserCreatedEvent event1 = new UserCreatedEvent("saga-1", 1L, "A", "B", "a@b.com", "ROLE_USER");
        UserCreatedEvent event2 = new UserCreatedEvent("saga-1", 1L, "A", "B", "a@b.com", "ROLE_USER");

        // Then
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }
}


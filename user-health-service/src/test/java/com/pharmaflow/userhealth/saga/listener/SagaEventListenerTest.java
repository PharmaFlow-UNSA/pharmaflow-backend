package com.pharmaflow.userhealth.saga.listener;

import com.pharmaflow.userhealth.saga.events.InventoryCreatedEvent;
import com.pharmaflow.userhealth.saga.events.InventoryCreationFailedEvent;
import com.pharmaflow.userhealth.saga.service.UserRegistrationSagaOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for SagaEventListener.
 * Tests event handling without requiring RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaEventListener - Unit Tests")
class SagaEventListenerTest {

    @Mock
    private UserRegistrationSagaOrchestrator sagaOrchestrator;

    private SagaEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SagaEventListener(sagaOrchestrator);
    }

    @Test
    @DisplayName("handleInventoryCreatedEvent should delegate to orchestrator")
    void handleInventoryCreatedEvent_shouldDelegateToOrchestrator() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;
        Long inventoryId = 42L;
        InventoryCreatedEvent event = new InventoryCreatedEvent(sagaId, userId, inventoryId);

        // When
        listener.handleInventoryCreatedEvent(event);

        // Then
        verify(sagaOrchestrator, times(1)).handleInventoryCreated(event);
    }

    @Test
    @DisplayName("handleInventoryCreatedEvent should handle exceptions gracefully")
    void handleInventoryCreatedEvent_shouldHandleExceptionsGracefully() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;
        Long inventoryId = 42L;
        InventoryCreatedEvent event = new InventoryCreatedEvent(sagaId, userId, inventoryId);

        doThrow(new RuntimeException("Database error"))
                .when(sagaOrchestrator).handleInventoryCreated(event);

        // When - should not throw exception
        listener.handleInventoryCreatedEvent(event);

        // Then - orchestrator was called
        verify(sagaOrchestrator, times(1)).handleInventoryCreated(event);
    }

    @Test
    @DisplayName("handleInventoryCreationFailedEvent should delegate to orchestrator")
    void handleInventoryCreationFailedEvent_shouldDelegateToOrchestrator() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;
        InventoryCreationFailedEvent event = new InventoryCreationFailedEvent(
                sagaId, userId, "Database error", "Connection timeout"
        );

        // When
        listener.handleInventoryCreationFailedEvent(event);

        // Then
        verify(sagaOrchestrator, times(1)).handleInventoryCreationFailed(event);
    }

    @Test
    @DisplayName("handleInventoryCreationFailedEvent should handle exceptions gracefully")
    void handleInventoryCreationFailedEvent_shouldHandleExceptionsGracefully() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;
        InventoryCreationFailedEvent event = new InventoryCreationFailedEvent(
                sagaId, userId, "Database error", "Connection timeout"
        );

        doThrow(new RuntimeException("Orchestrator error"))
                .when(sagaOrchestrator).handleInventoryCreationFailed(event);

        // When - should not throw exception
        listener.handleInventoryCreationFailedEvent(event);

        // Then - orchestrator was called
        verify(sagaOrchestrator, times(1)).handleInventoryCreationFailed(event);
    }

    @Test
    @DisplayName("Listener should process multiple events sequentially")
    void listener_shouldProcessMultipleEventsSequentially() {
        // Given
        InventoryCreatedEvent event1 = new InventoryCreatedEvent("saga-1", 1L, 10L);
        InventoryCreatedEvent event2 = new InventoryCreatedEvent("saga-2", 2L, 20L);
        InventoryCreationFailedEvent event3 = new InventoryCreationFailedEvent(
                "saga-3", 3L, "Error", "Details"
        );

        // When
        listener.handleInventoryCreatedEvent(event1);
        listener.handleInventoryCreatedEvent(event2);
        listener.handleInventoryCreationFailedEvent(event3);

        // Then
        verify(sagaOrchestrator, times(2)).handleInventoryCreated(any());
        verify(sagaOrchestrator, times(1)).handleInventoryCreationFailed(any());
    }
}



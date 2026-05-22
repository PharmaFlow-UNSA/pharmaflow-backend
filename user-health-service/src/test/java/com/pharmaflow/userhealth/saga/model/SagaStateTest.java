package com.pharmaflow.userhealth.saga.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SagaState entity.
 */
@DisplayName("SagaState Entity - Unit Tests")
class SagaStateTest {

    @Test
    @DisplayName("SagaState should track all saga lifecycle states")
    void sagaState_shouldHaveAllLifecycleStates() {
        // Given
        SagaState sagaState = new SagaState();

        // When/Then - All statuses should be available
        sagaState.setStatus(SagaState.SagaStatus.STARTED);
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.STARTED);

        sagaState.setStatus(SagaState.SagaStatus.USER_CREATED);
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.USER_CREATED);

        sagaState.setStatus(SagaState.SagaStatus.INVENTORY_PENDING);
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.INVENTORY_PENDING);

        sagaState.setStatus(SagaState.SagaStatus.COMPLETED);
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.COMPLETED);

        sagaState.setStatus(SagaState.SagaStatus.COMPENSATING);
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.COMPENSATING);

        sagaState.setStatus(SagaState.SagaStatus.COMPENSATED);
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.COMPENSATED);

        sagaState.setStatus(SagaState.SagaStatus.FAILED);
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.FAILED);
    }

    @Test
    @DisplayName("SagaState should initialize with required fields")
    void sagaState_shouldInitializeWithRequiredFields() {
        // Given/When
        SagaState sagaState = new SagaState();
        sagaState.setSagaId("saga-123");
        sagaState.setStatus(SagaState.SagaStatus.STARTED);
        sagaState.setSagaType("USER_REGISTRATION");
        sagaState.setEntityId(1L);
        sagaState.setCurrentStep("USER_CREATED");
        sagaState.setPayload("{\"userId\":1}");

        // Then
        assertThat(sagaState.getSagaId()).isEqualTo("saga-123");
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.STARTED);
        assertThat(sagaState.getSagaType()).isEqualTo("USER_REGISTRATION");
        assertThat(sagaState.getEntityId()).isEqualTo(1L);
        assertThat(sagaState.getCurrentStep()).isEqualTo("USER_CREATED");
        assertThat(sagaState.getPayload()).isEqualTo("{\"userId\":1}");
    }

    @Test
    @DisplayName("SagaState should support error tracking")
    void sagaState_shouldSupportErrorTracking() {
        // Given
        SagaState sagaState = new SagaState();
        String errorMessage = "Inventory creation failed: Database timeout";

        // When
        sagaState.setStatus(SagaState.SagaStatus.FAILED);
        sagaState.setErrorMessage(errorMessage);

        // Then
        assertThat(sagaState.getStatus()).isEqualTo(SagaState.SagaStatus.FAILED);
        assertThat(sagaState.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("SagaState status enum should have 7 values")
    void sagaStatus_shouldHaveSevenValues() {
        // When
        SagaState.SagaStatus[] statuses = SagaState.SagaStatus.values();

        // Then
        assertThat(statuses).hasSize(7);
        assertThat(statuses).contains(
                SagaState.SagaStatus.STARTED,
                SagaState.SagaStatus.USER_CREATED,
                SagaState.SagaStatus.INVENTORY_PENDING,
                SagaState.SagaStatus.COMPLETED,
                SagaState.SagaStatus.COMPENSATING,
                SagaState.SagaStatus.COMPENSATED,
                SagaState.SagaStatus.FAILED
        );
    }
}


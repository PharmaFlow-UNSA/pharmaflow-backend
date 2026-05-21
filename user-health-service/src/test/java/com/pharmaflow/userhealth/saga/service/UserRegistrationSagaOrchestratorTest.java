package com.pharmaflow.userhealth.saga.service;

import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.repositories.UserRepository;
import com.pharmaflow.userhealth.saga.events.InventoryCreatedEvent;
import com.pharmaflow.userhealth.saga.events.InventoryCreationFailedEvent;
import com.pharmaflow.userhealth.saga.model.SagaState;
import com.pharmaflow.userhealth.saga.publisher.SagaEventPublisher;
import com.pharmaflow.userhealth.saga.repository.SagaStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for UserRegistrationSagaOrchestrator.
 * Tests saga flow without requiring actual RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationSagaOrchestrator - Integration Tests")
class UserRegistrationSagaOrchestratorTest {

    @Mock
    private SagaStateRepository sagaStateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SagaEventPublisher eventPublisher;

    private UserRegistrationSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new UserRegistrationSagaOrchestrator(
                sagaStateRepository,
                userRepository,
                eventPublisher,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Test
    @DisplayName("startUserRegistrationSaga should create user and publish event")
    void startUserRegistrationSaga_shouldCreateUserAndPublishEvent() {
        // Given
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");
        user.setPassword("hashed");
        user.setRole("ROLE_USER");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFirstName("John");
        savedUser.setLastName("Doe");
        savedUser.setEmail("john@test.com");
        savedUser.setRole("ROLE_USER");

        when(sagaStateRepository.save(any(SagaState.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        String sagaId = orchestrator.startUserRegistrationSaga(user);

        // Then
        assertThat(sagaId).isNotNull();

        // Verify saga state was saved 3 times (STARTED, USER_CREATED, INVENTORY_PENDING)
        verify(sagaStateRepository, times(3)).save(any(SagaState.class));

        // Verify user was saved
        verify(userRepository, times(1)).save(any(User.class));

        // Verify UserCreatedEvent was published
        verify(eventPublisher, times(1)).publishUserCreatedEvent(any());
    }

    @Test
    @DisplayName("handleInventoryCreated should mark saga as COMPLETED")
    void handleInventoryCreated_shouldMarkSagaAsCompleted() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;
        Long inventoryId = 42L;

        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.INVENTORY_PENDING);
        sagaState.setEntityId(userId);

        when(sagaStateRepository.findBySagaId(sagaId)).thenReturn(Optional.of(sagaState));
        when(sagaStateRepository.save(any(SagaState.class))).thenAnswer(i -> i.getArgument(0));

        InventoryCreatedEvent event = new InventoryCreatedEvent(sagaId, userId, inventoryId);

        // When
        orchestrator.handleInventoryCreated(event);

        // Then
        ArgumentCaptor<SagaState> captor = ArgumentCaptor.forClass(SagaState.class);
        verify(sagaStateRepository).save(captor.capture());

        SagaState savedState = captor.getValue();
        assertThat(savedState.getStatus()).isEqualTo(SagaState.SagaStatus.COMPLETED);
        assertThat(savedState.getCurrentStep()).isEqualTo("COMPLETED");
        assertThat(savedState.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("handleInventoryCreationFailed should trigger compensation (user deletion)")
    void handleInventoryCreationFailed_shouldTriggerCompensation() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;

        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.INVENTORY_PENDING);
        sagaState.setEntityId(userId);

        User user = new User();
        user.setId(userId);
        user.setEmail("john@test.com");

        when(sagaStateRepository.findBySagaId(sagaId)).thenReturn(Optional.of(sagaState));
        when(sagaStateRepository.save(any(SagaState.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        InventoryCreationFailedEvent event = new InventoryCreationFailedEvent(
                sagaId, userId, "Database error", "Connection timeout"
        );

        // When
        orchestrator.handleInventoryCreationFailed(event);

        // Then
        // Verify saga state transitioned through COMPENSATING to COMPENSATED
        ArgumentCaptor<SagaState> captor = ArgumentCaptor.forClass(SagaState.class);
        verify(sagaStateRepository, atLeast(2)).save(captor.capture());

        // Last saved state should be COMPENSATED
        SagaState finalState = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalState.getStatus()).isEqualTo(SagaState.SagaStatus.COMPENSATED);
        assertThat(finalState.getErrorMessage()).contains("Database error");

        // Verify user was deleted
        verify(userRepository).deleteById(userId);

        // Verify UserDeletedEvent was published
        verify(eventPublisher).publishUserDeletedEvent(any());
    }

    @Test
    @DisplayName("handleInventoryCreated should be idempotent")
    void handleInventoryCreated_shouldBeIdempotent() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;

        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.COMPLETED); // Already completed

        when(sagaStateRepository.findBySagaId(sagaId)).thenReturn(Optional.of(sagaState));

        InventoryCreatedEvent event = new InventoryCreatedEvent(sagaId, userId, 42L);

        // When
        orchestrator.handleInventoryCreated(event);

        // Then - Should not save again (idempotent)
        verify(sagaStateRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleInventoryCreationFailed should be idempotent")
    void handleInventoryCreationFailed_shouldBeIdempotent() {
        // Given
        String sagaId = "test-saga-123";
        Long userId = 1L;

        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.COMPENSATED); // Already compensated

        when(sagaStateRepository.findBySagaId(sagaId)).thenReturn(Optional.of(sagaState));

        InventoryCreationFailedEvent event = new InventoryCreationFailedEvent(
                sagaId, userId, "Error", "Details"
        );

        // When
        orchestrator.handleInventoryCreationFailed(event);

        // Then - Should not process again (idempotent)
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getSagaState should return saga state by ID")
    void getSagaState_shouldReturnSagaStateById() {
        // Given
        String sagaId = "test-saga-123";
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.COMPLETED);

        when(sagaStateRepository.findBySagaId(sagaId)).thenReturn(Optional.of(sagaState));

        // When
        Optional<SagaState> result = orchestrator.getSagaState(sagaId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSagaId()).isEqualTo(sagaId);
        assertThat(result.get().getStatus()).isEqualTo(SagaState.SagaStatus.COMPLETED);
    }
}


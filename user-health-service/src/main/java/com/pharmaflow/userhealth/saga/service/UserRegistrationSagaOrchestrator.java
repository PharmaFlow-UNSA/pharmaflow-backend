package com.pharmaflow.userhealth.saga.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.repositories.UserRepository;
import com.pharmaflow.userhealth.saga.events.*;
import com.pharmaflow.userhealth.saga.model.SagaState;
import com.pharmaflow.userhealth.saga.publisher.SagaEventPublisher;
import com.pharmaflow.userhealth.saga.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Saga Orchestrator for User Registration flow.
 *
 * Flow:
 * 1. User creates account (Local Transaction 1)
 * 2. Publish UserCreatedEvent
 * 3. Wait for InventoryCreatedEvent or InventoryCreationFailedEvent
 * 4a. If success → Mark saga as COMPLETED
 * 4b. If failure → Delete user (Compensating Transaction) → Mark saga as COMPENSATED
 *
 * This implements Saga Choreography pattern where each service listens to events
 * and reacts accordingly without a central coordinator.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationSagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final UserRepository userRepository;
    private final SagaEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Start saga: Create user and publish event.
     * This is Local Transaction 1.
     */
    @Transactional
    public String startUserRegistrationSaga(User user) {
        String sagaId = UUID.randomUUID().toString();
        log.info("🚀 Starting User Registration Saga: sagaId={}", sagaId);

        try {
            // 1. Create saga state
            SagaState sagaState = createSagaState(sagaId, user);
            sagaStateRepository.save(sagaState);
            log.info("📝 Saga state created: sagaId={}, status={}", sagaId, sagaState.getStatus());

            // 2. Save user (Local Transaction 1)
            User savedUser = userRepository.save(user);
            log.info("✅ User saved: userId={}", savedUser.getId());

            // 3. Update saga state
            sagaState.setStatus(SagaState.SagaStatus.USER_CREATED);
            sagaState.setEntityId(savedUser.getId());
            sagaState.setCurrentStep("USER_CREATED");
            sagaStateRepository.save(sagaState);

            // 4. Publish UserCreatedEvent to trigger inventory creation (Local Transaction 2)
            UserCreatedEvent event = new UserCreatedEvent(
                    sagaId,
                    savedUser.getId(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getEmail(),
                    savedUser.getRole()
            );
            eventPublisher.publishUserCreatedEvent(event);

            // 5. Update saga state to INVENTORY_PENDING
            sagaState.setStatus(SagaState.SagaStatus.INVENTORY_PENDING);
            sagaState.setCurrentStep("INVENTORY_PENDING");
            sagaStateRepository.save(sagaState);

            log.info("✅ Saga initiated successfully: sagaId={}", sagaId);
            return sagaId;

        } catch (Exception e) {
            log.error("❌ Failed to start saga: sagaId={}, error={}", sagaId, e.getMessage(), e);
            markSagaAsFailed(sagaId, "Failed to start saga: " + e.getMessage());
            throw new RuntimeException("Failed to start user registration saga", e);
        }
    }

    /**
     * Handle successful inventory creation.
     * Marks saga as COMPLETED (final state).
     */
    @Transactional
    public void handleInventoryCreated(InventoryCreatedEvent event) {
        String sagaId = event.getSagaId();
        log.info("✅ Handling successful inventory creation: sagaId={}", sagaId);

        Optional<SagaState> sagaStateOpt = sagaStateRepository.findBySagaId(sagaId);

        if (sagaStateOpt.isEmpty()) {
            log.warn("⚠️  Saga state not found: sagaId={}", sagaId);
            return;
        }

        SagaState sagaState = sagaStateOpt.get();

        // Idempotency check
        if (sagaState.getStatus() == SagaState.SagaStatus.COMPLETED) {
            log.info("ℹ️  Saga already completed (idempotent): sagaId={}", sagaId);
            return;
        }

        // Mark as COMPLETED
        sagaState.setStatus(SagaState.SagaStatus.COMPLETED);
        sagaState.setCurrentStep("COMPLETED");
        sagaState.setCompletedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);

        log.info("🎉 Saga completed successfully: sagaId={}, userId={}", sagaId, event.getUserId());
    }

    /**
     * Handle inventory creation failure.
     * Triggers compensating transaction (delete user).
     */
    @Transactional
    public void handleInventoryCreationFailed(InventoryCreationFailedEvent event) {
        String sagaId = event.getSagaId();
        log.warn("⚠️  Handling inventory creation failure: sagaId={}, reason={}",
                sagaId, event.getReason());

        Optional<SagaState> sagaStateOpt = sagaStateRepository.findBySagaId(sagaId);

        if (sagaStateOpt.isEmpty()) {
            log.warn("⚠️  Saga state not found: sagaId={}", sagaId);
            return;
        }

        SagaState sagaState = sagaStateOpt.get();

        // Idempotency check
        if (sagaState.getStatus() == SagaState.SagaStatus.COMPENSATED ||
            sagaState.getStatus() == SagaState.SagaStatus.COMPENSATING) {
            log.info("ℹ️  Saga already compensating/compensated (idempotent): sagaId={}", sagaId);
            return;
        }

        try {
            // 1. Mark saga as COMPENSATING
            sagaState.setStatus(SagaState.SagaStatus.COMPENSATING);
            sagaState.setCurrentStep("COMPENSATING");
            sagaState.setErrorMessage(event.getReason() + ": " + event.getErrorDetails());
            sagaStateRepository.save(sagaState);

            // 2. Delete user (Compensating Transaction)
            Long userId = event.getUserId();
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isPresent()) {
                userRepository.deleteById(userId);
                log.info("🔄 User deleted as compensation: userId={}", userId);

                // 3. Publish UserDeletedEvent
                UserDeletedEvent userDeletedEvent = new UserDeletedEvent(
                        sagaId,
                        userId,
                        "COMPENSATION"
                );
                eventPublisher.publishUserDeletedEvent(userDeletedEvent);

                // 4. Mark saga as COMPENSATED
                sagaState.setStatus(SagaState.SagaStatus.COMPENSATED);
                sagaState.setCurrentStep("COMPENSATED");
                sagaState.setCompletedAt(LocalDateTime.now());
                sagaStateRepository.save(sagaState);

                log.info("✅ Saga compensated successfully: sagaId={}", sagaId);
            } else {
                log.warn("⚠️  User not found for compensation: userId={}", userId);
                sagaState.setStatus(SagaState.SagaStatus.FAILED);
                sagaState.setErrorMessage("User not found for compensation");
                sagaStateRepository.save(sagaState);
            }

        } catch (Exception e) {
            log.error("❌ Failed to compensate saga: sagaId={}, error={}", sagaId, e.getMessage(), e);
            markSagaAsFailed(sagaId, "Compensation failed: " + e.getMessage());
        }
    }

    /**
     * Create initial saga state.
     */
    private SagaState createSagaState(String sagaId, User user) {
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.STARTED);
        sagaState.setSagaType("USER_REGISTRATION");
        sagaState.setCurrentStep("STARTED");

        try {
            sagaState.setPayload(objectMapper.writeValueAsString(user));
        } catch (JsonProcessingException e) {
            log.warn("⚠️  Failed to serialize user payload: {}", e.getMessage());
            sagaState.setPayload("{}");
        }

        return sagaState;
    }

    /**
     * Mark saga as permanently failed.
     */
    private void markSagaAsFailed(String sagaId, String errorMessage) {
        sagaStateRepository.findBySagaId(sagaId).ifPresent(sagaState -> {
            sagaState.setStatus(SagaState.SagaStatus.FAILED);
            sagaState.setErrorMessage(errorMessage);
            sagaState.setCompletedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);
            log.error("❌ Saga marked as FAILED: sagaId={}", sagaId);
        });
    }

    /**
     * Get saga state by ID.
     */
    public Optional<SagaState> getSagaState(String sagaId) {
        return sagaStateRepository.findBySagaId(sagaId);
    }
}


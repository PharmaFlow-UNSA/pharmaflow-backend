package com.pharmaflow.userhealth.saga.controller;

import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.saga.model.SagaState;
import com.pharmaflow.userhealth.saga.service.UserRegistrationSagaOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for testing Saga Choreography pattern.
 * Provides endpoints to trigger saga transactions and check their status.
 */
@RestController
@RequestMapping("/api/saga")
@RequiredArgsConstructor
@Tag(name = "Saga Management", description = "Endpoints for testing Saga Choreography pattern")
public class SagaController {

    private final UserRegistrationSagaOrchestrator sagaOrchestrator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Start user registration saga.
     * Creates user and triggers inventory account creation in pharmacy service.
     */
    @PostMapping("/register-user")
    @Operation(summary = "Register user with saga",
               description = "Creates user and triggers inventory account creation. Returns sagaId for tracking.")
    public ResponseEntity<?> registerUserWithSaga(@RequestBody UserRegistrationRequest request) {
        try {
            // Create user entity
            User user = new User();
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setEmail(request.email());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setRole(request.role() != null ? request.role() : "ROLE_USER");

            // Start saga
            String sagaId = sagaOrchestrator.startUserRegistrationSaga(user);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "message", "User registration saga initiated",
                    "sagaId", sagaId,
                    "status", "PENDING",
                    "note", "Check saga status at /api/saga/status/" + sagaId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to initiate saga",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get saga status by sagaId.
     */
    @GetMapping("/status/{sagaId}")
    @Operation(summary = "Get saga status", description = "Check the current status of a saga transaction")
    public ResponseEntity<?> getSagaStatus(@PathVariable String sagaId) {
        Optional<SagaState> sagaStateOpt = sagaOrchestrator.getSagaState(sagaId);

        if (sagaStateOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Saga not found",
                    "sagaId", sagaId
            ));
        }

        SagaState sagaState = sagaStateOpt.get();

        return ResponseEntity.ok(Map.of(
                "sagaId", sagaState.getSagaId(),
                "status", sagaState.getStatus(),
                "currentStep", sagaState.getCurrentStep(),
                "entityId", sagaState.getEntityId(),
                "errorMessage", sagaState.getErrorMessage() != null ? sagaState.getErrorMessage() : "",
                "createdAt", sagaState.getCreatedAt(),
                "completedAt", sagaState.getCompletedAt() != null ? sagaState.getCompletedAt() : ""
        ));
    }

    /**
     * Request DTO for user registration.
     */
    public record UserRegistrationRequest(
            String firstName,
            String lastName,
            String email,
            String password,
            String role
    ) {}
}



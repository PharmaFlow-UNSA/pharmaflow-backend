package com.pharmaflow.producthealth.saga.controller;

import com.pharmaflow.producthealth.dto.ProductCreateDTO;
import com.pharmaflow.producthealth.saga.model.SagaState;
import com.pharmaflow.producthealth.saga.service.ProductCreationSagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for testing Product Creation Saga Choreography pattern.
 * Provides endpoints to trigger saga transactions and monitor their status.
 */
@RestController
@RequestMapping("/api/saga")
@RequiredArgsConstructor
@Tag(name = "Saga Management", description = "Endpoints for testing Product Creation Saga Choreography pattern")
public class SagaController {

    private final ProductCreationSagaService sagaService;

    /**
     * Start product creation saga.
     * Saves product locally and triggers stock entry creation in pharmacy-inventory-service.
     * Returns sagaId for status tracking.
     */
    @PostMapping("/create-product")
    @Operation(
        summary = "Create product with saga",
        description = "Creates product (Local Transaction 1) and triggers stock entry creation " +
                      "in pharmacy-inventory-service (Local Transaction 2) via RabbitMQ. " +
                      "Returns sagaId for tracking. If stock creation fails, product is deleted (compensation)."
    )
    public ResponseEntity<?> createProductWithSaga(@Valid @RequestBody ProductCreateDTO dto) {
        try {
            String sagaId = sagaService.startProductCreationSaga(dto);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "message", "Product creation saga initiated",
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
    @Operation(
        summary = "Get saga status",
        description = "Check the current status of a product creation saga. " +
                      "Possible statuses: STARTED, PRODUCT_CREATED, STOCK_PENDING, COMPLETED, COMPENSATING, COMPENSATED, FAILED"
    )
    public ResponseEntity<?> getSagaStatus(@PathVariable String sagaId) {
        Optional<SagaState> sagaStateOpt = sagaService.getSagaState(sagaId);

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
                "sagaType", sagaState.getSagaType(),
                "currentStep", sagaState.getCurrentStep(),
                "productId", sagaState.getEntityId() != null ? sagaState.getEntityId() : "",
                "errorMessage", sagaState.getErrorMessage() != null ? sagaState.getErrorMessage() : "",
                "createdAt", sagaState.getCreatedAt(),
                "completedAt", sagaState.getCompletedAt() != null ? sagaState.getCompletedAt() : ""
        ));
    }
}

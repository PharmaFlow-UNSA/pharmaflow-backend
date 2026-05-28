package com.pharmaflow.orderprescription.controller;

import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.OrderFulfillmentSaga;
import com.pharmaflow.orderprescription.service.OrderFulfillmentSagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoints for triggering and inspecting the order-fulfillment saga.
 * Mirrors the pattern used by {@code SagaController} in user-health-service.
 */
@RestController
@RequestMapping("/api/orders/saga")
@Tag(name = "Order Fulfillment Saga",
        description = "Endpoints to start the order-fulfillment saga (Zadatak 8.3) and check its status")
public class OrderSagaController {

    private final OrderFulfillmentSagaService sagaService;

    public OrderSagaController(OrderFulfillmentSagaService sagaService) {
        this.sagaService = sagaService;
    }

    @PostMapping("/place")
    @Operation(summary = "Place order via saga",
            description = "Creates the order locally and publishes order.placed so pharmacy-inventory "
                    + "reserves stock. Returns a correlationId for tracking the saga.")
    public ResponseEntity<Map<String, Object>> placeOrder(
            @Valid @RequestBody OrderCreateDTO request,
            @RequestParam @NotNull Long pharmacyId) {

        String correlationId = sagaService.startSaga(request, pharmacyId);

        Map<String, Object> body = new HashMap<>();
        body.put("correlationId", correlationId);
        body.put("status", "PENDING");
        body.put("note", "Track status at GET /api/orders/saga/status/" + correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @PostMapping("/{correlationId}/cancel")
    @Operation(summary = "Cancel a confirmed order via saga",
            description = "Publishes order.cancelled so pharmacy-inventory restocks. Only valid when "
                    + "the saga is in the COMPLETED state.")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String correlationId,
            @RequestParam(defaultValue = "USER_CANCELLED") String reason) {
        sagaService.cancelConfirmedOrder(correlationId, reason);
        Map<String, Object> body = new HashMap<>();
        body.put("correlationId", correlationId);
        body.put("status", "CANCELLING");
        return ResponseEntity.accepted().body(body);
    }

    @GetMapping("/status/{correlationId}")
    @Operation(summary = "Get saga status", description = "Current state of an order-fulfillment saga")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String correlationId) {
        OrderFulfillmentSaga saga = sagaService.findByCorrelationId(correlationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Saga not found for correlationId: " + correlationId));

        Map<String, Object> body = new HashMap<>();
        body.put("correlationId", saga.getCorrelationId());
        body.put("orderId", saga.getOrderId());
        body.put("pharmacyId", saga.getPharmacyId());
        body.put("status", saga.getStatus());
        body.put("currentStep", saga.getCurrentStep());
        body.put("errorMessage", saga.getErrorMessage());
        body.put("createdAt", saga.getCreatedAt());
        body.put("updatedAt", saga.getUpdatedAt());
        body.put("completedAt", saga.getCompletedAt());
        return ResponseEntity.ok(body);
    }
}

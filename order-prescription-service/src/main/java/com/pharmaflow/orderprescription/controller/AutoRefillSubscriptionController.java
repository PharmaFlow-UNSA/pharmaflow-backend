package com.pharmaflow.orderprescription.controller;

import com.pharmaflow.orderprescription.dto.AutoRefillSubscriptionDTO;
import com.pharmaflow.orderprescription.service.AutoRefillSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auto-refill-subscriptions")
@Tag(name = "Auto-Refill Subscription Management", description = "APIs for managing auto-refill subscriptions")
public class AutoRefillSubscriptionController {

    private final AutoRefillSubscriptionService autoRefillSubscriptionService;

    public AutoRefillSubscriptionController(AutoRefillSubscriptionService autoRefillSubscriptionService) {
        this.autoRefillSubscriptionService = autoRefillSubscriptionService;
    }

    @GetMapping
    @Operation(summary = "Get all subscriptions", description = "Retrieves a list of all auto-refill subscriptions")
    public ResponseEntity<List<AutoRefillSubscriptionDTO>> getAllSubscriptions() {
        return ResponseEntity.ok(autoRefillSubscriptionService.getAllSubscriptions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subscription by ID", description = "Retrieves a specific auto-refill subscription by its ID")
    public ResponseEntity<AutoRefillSubscriptionDTO> getSubscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(autoRefillSubscriptionService.getSubscriptionById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get subscriptions by user ID", description = "Retrieves all auto-refill subscriptions for a specific user")
    public ResponseEntity<List<AutoRefillSubscriptionDTO>> getSubscriptionsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(autoRefillSubscriptionService.getSubscriptionsByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get subscriptions by status", description = "Retrieves all auto-refill subscriptions with a specific status")
    public ResponseEntity<List<AutoRefillSubscriptionDTO>> getSubscriptionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(autoRefillSubscriptionService.getSubscriptionsByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new subscription", description = "Creates a new auto-refill subscription with calculated interval")
    public ResponseEntity<AutoRefillSubscriptionDTO> createSubscription(@Valid @RequestBody AutoRefillSubscriptionDTO autoRefillSubscriptionDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autoRefillSubscriptionService.createSubscription(autoRefillSubscriptionDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subscription", description = "Updates an existing auto-refill subscription")
    public ResponseEntity<AutoRefillSubscriptionDTO> updateSubscription(@PathVariable Long id, @Valid @RequestBody AutoRefillSubscriptionDTO autoRefillSubscriptionDTO) {
        return ResponseEntity.ok(autoRefillSubscriptionService.updateSubscription(id, autoRefillSubscriptionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subscription", description = "Deletes an auto-refill subscription by ID")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        autoRefillSubscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}

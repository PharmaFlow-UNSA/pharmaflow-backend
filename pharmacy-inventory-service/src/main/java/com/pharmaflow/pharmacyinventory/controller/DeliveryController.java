package com.pharmaflow.pharmacyinventory.controller;

import com.pharmaflow.pharmacyinventory.dto.DeliveryDTO;
import com.pharmaflow.pharmacyinventory.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Delivery Management", description = "APIs for managing deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    @Operation(
            summary = "Get all deliveries",
            description = "Supports pagination (?page=0&size=10&sort=id,asc) and optional filtering by status, orderId, pharmacyId."
    )
    public ResponseEntity<Page<DeliveryDTO>> getDeliveries(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long pharmacyId) {
        return ResponseEntity.ok(deliveryService.findAll(status, orderId, pharmacyId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery by ID", description = "Retrieves a specific delivery by its ID")
    public ResponseEntity<DeliveryDTO> getDeliveryById(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.getDeliveryById(id));
    }

    @GetMapping("/pharmacy/{pharmacyId}")
    @Operation(summary = "Get deliveries by pharmacy ID", description = "Retrieves all deliveries for a specific pharmacy")
    public ResponseEntity<List<DeliveryDTO>> getDeliveriesByPharmacyId(@PathVariable Long pharmacyId) {
        return ResponseEntity.ok(deliveryService.getDeliveriesByPharmacyId(pharmacyId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get deliveries by order ID", description = "Retrieves all deliveries for a specific order")
    public ResponseEntity<List<DeliveryDTO>> getDeliveriesByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getDeliveriesByOrderId(orderId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get deliveries by status", description = "Retrieves all deliveries with a specific status")
    public ResponseEntity<List<DeliveryDTO>> getDeliveriesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(deliveryService.getDeliveriesByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new delivery", description = "Creates a new delivery for an order")
    public ResponseEntity<DeliveryDTO> createDelivery(@Valid @RequestBody DeliveryDTO deliveryDTO) {
        return new ResponseEntity<>(deliveryService.createDelivery(deliveryDTO), HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create deliveries", description = "Creates multiple deliveries in a single transaction")
    public ResponseEntity<List<DeliveryDTO>> createDeliveriesBatch(
            @RequestBody @Valid List<@Valid DeliveryDTO> dtos) {
        return new ResponseEntity<>(deliveryService.createDeliveriesBatch(dtos), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update delivery", description = "Updates an existing delivery")
    public ResponseEntity<DeliveryDTO> updateDelivery(@PathVariable Long id,
                                                      @Valid @RequestBody DeliveryDTO deliveryDTO) {
        return ResponseEntity.ok(deliveryService.updateDelivery(id, deliveryDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update delivery", description = "Applies JSON Patch operations (RFC 6902) to a delivery")
    public ResponseEntity<DeliveryDTO> patchDelivery(@PathVariable Long id,
                                                     @RequestBody String patchDocument) {
        return ResponseEntity.ok(deliveryService.patchDelivery(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete delivery", description = "Deletes a delivery by ID")
    public ResponseEntity<Void> deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
        return ResponseEntity.noContent().build();
    }
}

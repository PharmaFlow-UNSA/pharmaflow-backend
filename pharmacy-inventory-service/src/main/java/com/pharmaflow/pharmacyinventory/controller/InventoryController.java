package com.pharmaflow.pharmacyinventory.controller;

import com.pharmaflow.pharmacyinventory.dto.InventoryDTO;
import com.pharmaflow.pharmacyinventory.service.InventoryService;
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
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "APIs for managing pharmacy inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(
            summary = "Get all inventory items",
            description = "Supports pagination (?page=0&size=10&sort=id,asc) and optional filtering by pharmacyId, productId, minQuantity."
    )
    public ResponseEntity<Page<InventoryDTO>> getInventory(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long pharmacyId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer minQuantity) {
        return ResponseEntity.ok(inventoryService.findAll(pharmacyId, productId, minQuantity, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory by ID", description = "Retrieves a specific inventory item by its ID")
    public ResponseEntity<InventoryDTO> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/pharmacy/{pharmacyId}")
    @Operation(summary = "Get inventory by pharmacy ID", description = "Retrieves all inventory items for a specific pharmacy")
    public ResponseEntity<List<InventoryDTO>> getInventoryByPharmacyId(@PathVariable Long pharmacyId) {
        return ResponseEntity.ok(inventoryService.getInventoryByPharmacyId(pharmacyId));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory by product ID", description = "Retrieves inventory across all pharmacies for a specific product")
    public ResponseEntity<List<InventoryDTO>> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @PostMapping
    @Operation(summary = "Create inventory item", description = "Creates a new inventory item for a pharmacy")
    public ResponseEntity<InventoryDTO> createInventory(@Valid @RequestBody InventoryDTO inventoryDTO) {
        return new ResponseEntity<>(inventoryService.createInventory(inventoryDTO), HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create inventory items", description = "Creates multiple inventory items in a single transaction")
    public ResponseEntity<List<InventoryDTO>> createInventoriesBatch(
            @RequestBody @Valid List<@Valid InventoryDTO> dtos) {
        return new ResponseEntity<>(inventoryService.createInventoriesBatch(dtos), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inventory item", description = "Updates an existing inventory item")
    public ResponseEntity<InventoryDTO> updateInventory(@PathVariable Long id,
                                                        @Valid @RequestBody InventoryDTO inventoryDTO) {
        return ResponseEntity.ok(inventoryService.updateInventory(id, inventoryDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update inventory", description = "Applies JSON Patch operations (RFC 6902) to an inventory item")
    public ResponseEntity<InventoryDTO> patchInventory(@PathVariable Long id,
                                                       @RequestBody String patchDocument) {
        return ResponseEntity.ok(inventoryService.patchInventory(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inventory item", description = "Deletes an inventory item by ID")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }
}

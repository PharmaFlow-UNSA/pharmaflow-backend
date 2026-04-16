package com.pharmaflow.orderprescription.controller;

import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@Tag(name = "Prescription Management", description = "APIs for managing prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    @Operation(summary = "Get all prescriptions", description = "Retrieves a list of all prescriptions")
    public ResponseEntity<List<PrescriptionDTO>> getAllPrescriptions() {
        return ResponseEntity.ok(prescriptionService.getAllPrescriptions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID", description = "Retrieves a specific prescription by its ID")
    public ResponseEntity<PrescriptionDTO> getPrescriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get prescriptions by user ID", description = "Retrieves all prescriptions for a specific user")
    public ResponseEntity<List<PrescriptionDTO>> getPrescriptionsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get prescriptions by status", description = "Retrieves all prescriptions with a specific status")
    public ResponseEntity<List<PrescriptionDTO>> getPrescriptionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new prescription", description = "Uploads a new prescription for validation")
    public ResponseEntity<PrescriptionDTO> createPrescription(@Valid @RequestBody PrescriptionCreateDTO prescriptionCreateDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.createPrescription(prescriptionCreateDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update prescription", description = "Updates an existing prescription")
    public ResponseEntity<PrescriptionDTO> updatePrescription(@PathVariable Long id, @Valid @RequestBody PrescriptionDTO prescriptionDTO) {
        return ResponseEntity.ok(prescriptionService.updatePrescription(id, prescriptionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete prescription", description = "Deletes a prescription by ID")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }
}

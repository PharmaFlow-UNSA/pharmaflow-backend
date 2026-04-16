package com.pharmaflow.pharmacyinventory.controller;

import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.service.PharmacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacies")
@Tag(name = "Pharmacy Management", description = "APIs for managing pharmacies")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @GetMapping
    @Operation(summary = "Get all pharmacies", description = "Retrieves a list of all pharmacies")
    public ResponseEntity<List<PharmacyDTO>> getAllPharmacies() {
        return ResponseEntity.ok(pharmacyService.getAllPharmacies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pharmacy by ID", description = "Retrieves a specific pharmacy by its ID")
    public ResponseEntity<PharmacyDTO> getPharmacyById(@PathVariable Long id) {
        return ResponseEntity.ok(pharmacyService.getPharmacyById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new pharmacy", description = "Creates a new pharmacy")
    public ResponseEntity<PharmacyDTO> createPharmacy(@Valid @RequestBody PharmacyCreateDTO createDTO) {
        return new ResponseEntity<>(pharmacyService.createPharmacy(createDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pharmacy", description = "Updates an existing pharmacy")
    public ResponseEntity<PharmacyDTO> updatePharmacy(@PathVariable Long id,
                                                      @Valid @RequestBody PharmacyCreateDTO createDTO) {
        return ResponseEntity.ok(pharmacyService.updatePharmacy(id, createDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pharmacy", description = "Deletes a pharmacy by ID")
    public ResponseEntity<Void> deletePharmacy(@PathVariable Long id) {
        pharmacyService.deletePharmacy(id);
        return ResponseEntity.noContent().build();
    }
}

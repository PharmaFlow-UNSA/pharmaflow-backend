package com.pharmaflow.pharmacyinventory.controller;

import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.service.PharmacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(
            summary = "Get all pharmacies",
            description = "Supports pagination (?page=0&size=10&sort=id,asc) and optional filtering by name (substring) and city (exact)."
    )
    public ResponseEntity<Page<PharmacyDTO>> getPharmacies(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(pharmacyService.findAll(name, city, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pharmacy by ID", description = "Retrieves a specific pharmacy by its ID")
    public ResponseEntity<PharmacyDTO> getPharmacyById(@PathVariable Long id) {
        return ResponseEntity.ok(pharmacyService.getPharmacyById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new pharmacy", description = "Creates a new pharmacy")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PharmacyDTO> createPharmacy(@Valid @RequestBody PharmacyCreateDTO createDTO) {
        return new ResponseEntity<>(pharmacyService.createPharmacy(createDTO), HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create pharmacies", description = "Creates multiple pharmacies in a single transaction")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<PharmacyDTO>> createPharmaciesBatch(
            @RequestBody @Valid List<@Valid PharmacyCreateDTO> dtos) {
        return new ResponseEntity<>(pharmacyService.createPharmaciesBatch(dtos), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pharmacy", description = "Updates an existing pharmacy")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PharmacyDTO> updatePharmacy(@PathVariable Long id,
                                                      @Valid @RequestBody PharmacyCreateDTO createDTO) {
        return ResponseEntity.ok(pharmacyService.updatePharmacy(id, createDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update pharmacy", description = "Applies JSON Patch operations (RFC 6902) to a pharmacy")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PharmacyDTO> patchPharmacy(@PathVariable Long id,
                                                     @RequestBody String patchDocument) {
        return ResponseEntity.ok(pharmacyService.patchPharmacy(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pharmacy", description = "Deletes a pharmacy by ID")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<Void> deletePharmacy(@PathVariable Long id) {
        pharmacyService.deletePharmacy(id);
        return ResponseEntity.noContent().build();
    }
}

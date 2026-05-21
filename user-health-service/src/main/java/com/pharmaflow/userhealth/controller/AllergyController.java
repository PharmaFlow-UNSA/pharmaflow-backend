package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.models.enums.Severity;
import com.pharmaflow.userhealth.service.AllergyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allergies")
@Tag(name = "Allergy Management", description = "APIs for managing patient allergies")
public class AllergyController {

    private final AllergyService allergyService;

    public AllergyController(AllergyService allergyService) {
        this.allergyService = allergyService;
    }

    @GetMapping
    @Operation(
        summary = "Get all allergies",
        description = "Supports pagination (?page=0&size=20&sort=allergen,asc) and optional filtering by severity (LOW, MODERATE, HIGH, SEVERE, LIFE_THREATENING) or allergen name."
    )
    public ResponseEntity<Page<AllergyDTO>> getAllergies(
            @PageableDefault(size = 20, sort = "allergen") Pageable pageable,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String allergen) {
        return ResponseEntity.ok(allergyService.findAll(severity, allergen, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get allergy by ID", description = "Retrieves a specific allergy by ID")
    public ResponseEntity<AllergyDTO> getAllergyById(@PathVariable Long id) {
        return ResponseEntity.ok(allergyService.getAllergyById(id));
    }

    @PostMapping
    @Operation(summary = "Create an allergy", description = "Creates a new allergy record")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<AllergyDTO> createAllergy(@Valid @RequestBody AllergyDTO allergyDTO) {
        AllergyDTO createdAllergy = allergyService.createAllergy(allergyDTO);
        return new ResponseEntity<>(createdAllergy, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create allergies", description = "Creates multiple allergy records in a single transaction")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<AllergyDTO>> createAllergiesBatch(
            @RequestBody @Valid List<@Valid AllergyDTO> allergyDTOs) {
        List<AllergyDTO> createdAllergies = allergyService.createAllergiesBatch(allergyDTOs);
        return new ResponseEntity<>(createdAllergies, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update allergy", description = "Updates an existing allergy record")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<AllergyDTO> updateAllergy(@PathVariable Long id,
                                                    @Valid @RequestBody AllergyDTO allergyDTO) {
        return ResponseEntity.ok(allergyService.updateAllergy(id, allergyDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update allergy", description = "Applies JSON Patch operations to an allergy")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<AllergyDTO> patchAllergy(@PathVariable Long id,
                                                   @RequestBody String patchDocument) {
        return ResponseEntity.ok(allergyService.patchAllergy(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete allergy", description = "Deletes an allergy record by ID")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteAllergy(@PathVariable Long id) {
        allergyService.deleteAllergy(id);
        return ResponseEntity.noContent().build();
    }
}


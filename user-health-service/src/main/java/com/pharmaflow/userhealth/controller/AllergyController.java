package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.service.AllergyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Get all allergies", description = "Retrieves a list of all allergies")
    public ResponseEntity<List<AllergyDTO>> getAllAllergies() {
        return ResponseEntity.ok(allergyService.getAllAllergies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get allergy by ID", description = "Retrieves a specific allergy by ID")
    public ResponseEntity<AllergyDTO> getAllergyById(@PathVariable Long id) {
        return ResponseEntity.ok(allergyService.getAllergyById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new allergy", description = "Creates a new allergy record")
    public ResponseEntity<AllergyDTO> createAllergy(@Valid @RequestBody AllergyDTO allergyDTO) {
        AllergyDTO createdAllergy = allergyService.createAllergy(allergyDTO);
        return new ResponseEntity<>(createdAllergy, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update allergy", description = "Updates an existing allergy record")
    public ResponseEntity<AllergyDTO> updateAllergy(@PathVariable Long id,
                                                    @Valid @RequestBody AllergyDTO allergyDTO) {
        return ResponseEntity.ok(allergyService.updateAllergy(id, allergyDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete allergy", description = "Deletes an allergy record by ID")
    public ResponseEntity<Void> deleteAllergy(@PathVariable Long id) {
        allergyService.deleteAllergy(id);
        return ResponseEntity.noContent().build();
    }
}


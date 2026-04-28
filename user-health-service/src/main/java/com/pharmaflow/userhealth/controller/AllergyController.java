package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.service.AllergyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    @Operation(summary = "Get all allergies", description = "Retrieves allergies with optional pagination, sorting, and filtering. Use ?page=0&size=20&sort=allergen,asc for pagination. Use ?severity=HIGH or ?allergen=peanut for filtering.")
    public ResponseEntity<List<AllergyDTO>> getAllergies(
            @PageableDefault(size = 20, sort = "allergen") Pageable pageable,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String allergen,
            @RequestParam(required = false) Integer page) {

        // If filtering by severity
        if (severity != null && !severity.isEmpty()) {
            return ResponseEntity.ok(allergyService.findBySeverity(severity));
        }

        // If filtering by allergen name
        if (allergen != null && !allergen.isEmpty()) {
            return ResponseEntity.ok(allergyService.findByAllergenContaining(allergen));
        }

        // If pagination is explicitly requested
        if (page != null) {
            return ResponseEntity.ok(allergyService.getAllergiesPaginated(pageable).getContent());
        }

        // Default: return all allergies
        return ResponseEntity.ok(allergyService.getAllAllergies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get allergy by ID", description = "Retrieves a specific allergy by ID")
    public ResponseEntity<AllergyDTO> getAllergyById(@PathVariable Long id) {
        return ResponseEntity.ok(allergyService.getAllergyById(id));
    }

    @PostMapping
    @Operation(summary = "Create an allergy", description = "Creates a new allergy record")
    public ResponseEntity<AllergyDTO> createAllergy(@Valid @RequestBody AllergyDTO allergyDTO) {
        AllergyDTO createdAllergy = allergyService.createAllergy(allergyDTO);
        return new ResponseEntity<>(createdAllergy, HttpStatus.CREATED);
    }

    @PostMapping(params = "bulk")
    @Operation(summary = "Create multiple allergies (bulk)", description = "Bulk creation of allergy records. Use ?bulk=true")
    public ResponseEntity<List<AllergyDTO>> createAllergiesBulk(
            @RequestBody @Valid List<@Valid AllergyDTO> allergyDTOs) {
        List<AllergyDTO> createdAllergies = allergyService.createAllergiesBatch(allergyDTOs);
        return new ResponseEntity<>(createdAllergies, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update allergy", description = "Updates an existing allergy record")
    public ResponseEntity<AllergyDTO> updateAllergy(@PathVariable Long id,
                                                    @Valid @RequestBody AllergyDTO allergyDTO) {
        return ResponseEntity.ok(allergyService.updateAllergy(id, allergyDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update allergy", description = "Applies JSON Patch operations to an allergy")
    public ResponseEntity<AllergyDTO> patchAllergy(@PathVariable Long id,
                                                   @RequestBody String patchDocument) {
        return ResponseEntity.ok(allergyService.patchAllergy(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete allergy", description = "Deletes an allergy record by ID")
    public ResponseEntity<Void> deleteAllergy(@PathVariable Long id) {
        allergyService.deleteAllergy(id);
        return ResponseEntity.noContent().build();
    }
}


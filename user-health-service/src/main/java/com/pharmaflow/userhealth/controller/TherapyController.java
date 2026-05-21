package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.service.TherapyService;
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
@RequestMapping("/api/therapies")
@Tag(name = "Therapy Management", description = "APIs for managing patient therapies")
public class TherapyController {

    private final TherapyService therapyService;

    public TherapyController(TherapyService therapyService) {
        this.therapyService = therapyService;
    }

    @GetMapping
    @Operation(
        summary = "Get all therapies",
        description = "Supports pagination (?page=0&size=20&sort=medicationName,asc) and optional filtering by medication name."
    )
    public ResponseEntity<Page<TherapyDTO>> getTherapies(
            @PageableDefault(size = 20, sort = "medicationName") Pageable pageable,
            @RequestParam(required = false) String medicationName) {
        return ResponseEntity.ok(therapyService.findAll(medicationName, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get therapy by ID", description = "Retrieves a specific therapy by ID")
    public ResponseEntity<TherapyDTO> getTherapyById(@PathVariable Long id) {
        return ResponseEntity.ok(therapyService.getTherapyById(id));
    }

    @PostMapping
    @Operation(summary = "Create a therapy", description = "Creates a new therapy record")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<TherapyDTO> createTherapy(@Valid @RequestBody TherapyDTO therapyDTO) {
        TherapyDTO createdTherapy = therapyService.createTherapy(therapyDTO);
        return new ResponseEntity<>(createdTherapy, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create therapies", description = "Creates multiple therapy records in a single transaction")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<TherapyDTO>> createTherapiesBatch(
            @RequestBody @Valid List<@Valid TherapyDTO> therapyDTOs) {
        List<TherapyDTO> createdTherapies = therapyService.createTherapiesBatch(therapyDTOs);
        return new ResponseEntity<>(createdTherapies, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update therapy", description = "Updates an existing therapy record")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<TherapyDTO> updateTherapy(@PathVariable Long id,
                                                    @Valid @RequestBody TherapyDTO therapyDTO) {
        return ResponseEntity.ok(therapyService.updateTherapy(id, therapyDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update therapy", description = "Applies JSON Patch operations to a therapy")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<TherapyDTO> patchTherapy(@PathVariable Long id,
                                                   @RequestBody String patchDocument) {
        return ResponseEntity.ok(therapyService.patchTherapy(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete therapy", description = "Deletes a therapy record by ID")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteTherapy(@PathVariable Long id) {
        therapyService.deleteTherapy(id);
        return ResponseEntity.noContent().build();
    }
}


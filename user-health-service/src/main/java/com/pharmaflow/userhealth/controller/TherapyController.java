package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.service.TherapyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Get all therapies", description = "Retrieves a list of all therapies")
    public ResponseEntity<List<TherapyDTO>> getAllTherapies() {
        return ResponseEntity.ok(therapyService.getAllTherapies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get therapy by ID", description = "Retrieves a specific therapy by ID")
    public ResponseEntity<TherapyDTO> getTherapyById(@PathVariable Long id) {
        return ResponseEntity.ok(therapyService.getTherapyById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new therapy", description = "Creates a new therapy record")
    public ResponseEntity<TherapyDTO> createTherapy(@Valid @RequestBody TherapyDTO therapyDTO) {
        TherapyDTO createdTherapy = therapyService.createTherapy(therapyDTO);
        return new ResponseEntity<>(createdTherapy, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update therapy", description = "Updates an existing therapy record")
    public ResponseEntity<TherapyDTO> updateTherapy(@PathVariable Long id,
                                                    @Valid @RequestBody TherapyDTO therapyDTO) {
        return ResponseEntity.ok(therapyService.updateTherapy(id, therapyDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete therapy", description = "Deletes a therapy record by ID")
    public ResponseEntity<Void> deleteTherapy(@PathVariable Long id) {
        therapyService.deleteTherapy(id);
        return ResponseEntity.noContent().build();
    }
}


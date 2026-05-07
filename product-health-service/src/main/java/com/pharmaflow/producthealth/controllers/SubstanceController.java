package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.dto.SubstanceDTO;
import com.pharmaflow.producthealth.services.SubstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/substances")
@RequiredArgsConstructor
@Tag(name = "Substances", description = "API for managing active substances")
public class SubstanceController {

    private final SubstanceService substanceService;

    @GetMapping
    @Operation(summary = "Get all substances")
    public ResponseEntity<List<SubstanceDTO>> getAllSubstances() {
        return ResponseEntity.ok(substanceService.getAllSubstances());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get substance by ID")
    public ResponseEntity<SubstanceDTO> getSubstanceById(@PathVariable Long id) {
        return ResponseEntity.ok(substanceService.getSubstanceById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new substance")
    public ResponseEntity<SubstanceDTO> createSubstance(@Valid @RequestBody SubstanceDTO dto) {
        return new ResponseEntity<>(substanceService.createSubstance(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update substance")
    public ResponseEntity<SubstanceDTO> updateSubstance(@PathVariable Long id,
                                                        @Valid @RequestBody SubstanceDTO dto) {
        return ResponseEntity.ok(substanceService.updateSubstance(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete substance")
    public ResponseEntity<Void> deleteSubstance(@PathVariable Long id) {
        substanceService.deleteSubstance(id);
        return ResponseEntity.noContent().build();
    }
}
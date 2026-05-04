package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.dto.ContraindicationDTO;
import com.pharmaflow.producthealth.services.ContraindicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contraindications")
@RequiredArgsConstructor
@Tag(name = "Contraindications", description = "API for managing substance contraindications")
public class ContraindicationController {

    private final ContraindicationService contraindicationService;

    @GetMapping
    @Operation(summary = "Get all contraindications")
    public ResponseEntity<List<ContraindicationDTO>> getAllContraindications() {
        return ResponseEntity.ok(contraindicationService.getAllContraindications());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contraindication by ID")
    public ResponseEntity<ContraindicationDTO> getContraindicationById(@PathVariable Long id) {
        return ResponseEntity.ok(contraindicationService.getContraindicationById(id));
    }

    @GetMapping("/substance/{substanceId}")
    @Operation(summary = "Get contraindications for a specific substance")
    public ResponseEntity<List<ContraindicationDTO>> getBySubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(contraindicationService.getBySubstance(substanceId));
    }

    @PostMapping
    @Operation(summary = "Create a new contraindication")
    public ResponseEntity<ContraindicationDTO> createContraindication(@Valid @RequestBody ContraindicationDTO dto) {
        return new ResponseEntity<>(contraindicationService.createContraindication(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update contraindication")
    public ResponseEntity<ContraindicationDTO> updateContraindication(@PathVariable Long id,
                                                                      @Valid @RequestBody ContraindicationDTO dto) {
        return ResponseEntity.ok(contraindicationService.updateContraindication(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete contraindication")
    public ResponseEntity<Void> deleteContraindication(@PathVariable Long id) {
        contraindicationService.deleteContraindication(id);
        return ResponseEntity.noContent().build();
    }
}
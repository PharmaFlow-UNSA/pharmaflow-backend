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
@Tag(name = "Substances", description = "API za upravljanje aktivnim supstancama")
public class SubstanceController {

    private final SubstanceService substanceService;

    @GetMapping
    @Operation(summary = "Dohvati sve supstance")
    public ResponseEntity<List<SubstanceDTO>> getAllSubstances() {
        return ResponseEntity.ok(substanceService.getAllSubstances());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dohvati supstancu po ID-u")
    public ResponseEntity<SubstanceDTO> getSubstanceById(@PathVariable Long id) {
        return ResponseEntity.ok(substanceService.getSubstanceById(id));
    }

    @PostMapping
    @Operation(summary = "Kreiraj novu supstancu")
    public ResponseEntity<SubstanceDTO> createSubstance(@Valid @RequestBody SubstanceDTO dto) {
        return new ResponseEntity<>(substanceService.createSubstance(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ažuriraj supstancu")
    public ResponseEntity<SubstanceDTO> updateSubstance(@PathVariable Long id,
                                                         @Valid @RequestBody SubstanceDTO dto) {
        return ResponseEntity.ok(substanceService.updateSubstance(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Obriši supstancu")
    public ResponseEntity<Void> deleteSubstance(@PathVariable Long id) {
        substanceService.deleteSubstance(id);
        return ResponseEntity.noContent().build();
    }
}

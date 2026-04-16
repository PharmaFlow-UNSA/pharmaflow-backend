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
@Tag(name = "Contraindications", description = "API za upravljanje kontraindikacijama supstanci")
public class ContraindicationController {

    private final ContraindicationService contraindicationService;

    @GetMapping
    @Operation(summary = "Dohvati sve kontraindikacije")
    public ResponseEntity<List<ContraindicationDTO>> getAllContraindications() {
        return ResponseEntity.ok(contraindicationService.getAllContraindications());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dohvati kontraindikaciju po ID-u")
    public ResponseEntity<ContraindicationDTO> getContraindicationById(@PathVariable Long id) {
        return ResponseEntity.ok(contraindicationService.getContraindicationById(id));
    }

    @GetMapping("/substance/{substanceId}")
    @Operation(summary = "Dohvati kontraindikacije za datu supstancu")
    public ResponseEntity<List<ContraindicationDTO>> getBySubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(contraindicationService.getBySubstance(substanceId));
    }

    @PostMapping
    @Operation(summary = "Kreiraj novu kontraindikaciju")
    public ResponseEntity<ContraindicationDTO> createContraindication(@Valid @RequestBody ContraindicationDTO dto) {
        return new ResponseEntity<>(contraindicationService.createContraindication(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ažuriraj kontraindikaciju")
    public ResponseEntity<ContraindicationDTO> updateContraindication(@PathVariable Long id,
                                                                       @Valid @RequestBody ContraindicationDTO dto) {
        return ResponseEntity.ok(contraindicationService.updateContraindication(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Obriši kontraindikaciju")
    public ResponseEntity<Void> deleteContraindication(@PathVariable Long id) {
        contraindicationService.deleteContraindication(id);
        return ResponseEntity.noContent().build();
    }
}

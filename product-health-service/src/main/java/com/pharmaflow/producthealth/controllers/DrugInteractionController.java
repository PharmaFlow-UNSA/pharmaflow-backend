package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.dto.DrugInteractionDTO;
import com.pharmaflow.producthealth.services.DrugInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
@Tag(name = "Drug Interactions", description = "API za provjeru interakcija između lijekova")
public class DrugInteractionController {

    private final DrugInteractionService drugInteractionService;

    @GetMapping
    @Operation(summary = "Dohvati sve interakcije lijekova")
    public ResponseEntity<List<DrugInteractionDTO>> getAllInteractions() {
        return ResponseEntity.ok(drugInteractionService.getAllInteractions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dohvati interakciju po ID-u")
    public ResponseEntity<DrugInteractionDTO> getInteractionById(@PathVariable Long id) {
        return ResponseEntity.ok(drugInteractionService.getInteractionById(id));
    }

    @GetMapping("/substance/{substanceId}")
    @Operation(summary = "Dohvati sve interakcije za datu supstancu")
    public ResponseEntity<List<DrugInteractionDTO>> getForSubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(drugInteractionService.getInteractionsForSubstance(substanceId));
    }

    @GetMapping("/check")
    @Operation(summary = "Provjeri interakciju između dvije supstance")
    public ResponseEntity<List<DrugInteractionDTO>> checkInteraction(
            @RequestParam Long substanceAId, @RequestParam Long substanceBId) {
        return ResponseEntity.ok(drugInteractionService.checkInteractionBetween(substanceAId, substanceBId));
    }

    @PostMapping
    @Operation(summary = "Kreiraj novu interakciju lijekova")
    public ResponseEntity<DrugInteractionDTO> createInteraction(@Valid @RequestBody DrugInteractionDTO dto) {
        return new ResponseEntity<>(drugInteractionService.createInteraction(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ažuriraj interakciju lijekova")
    public ResponseEntity<DrugInteractionDTO> updateInteraction(@PathVariable Long id,
                                                                  @Valid @RequestBody DrugInteractionDTO dto) {
        return ResponseEntity.ok(drugInteractionService.updateInteraction(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Obriši interakciju lijekova")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
        drugInteractionService.deleteInteraction(id);
        return ResponseEntity.noContent().build();
    }
}

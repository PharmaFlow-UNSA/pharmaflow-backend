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
@Tag(name = "Drug Interactions", description = "API for checking drug interactions")
public class DrugInteractionController {

    private final DrugInteractionService drugInteractionService;

    @GetMapping
    @Operation(summary = "Get all drug interactions")
    public ResponseEntity<List<DrugInteractionDTO>> getAllInteractions() {
        return ResponseEntity.ok(drugInteractionService.getAllInteractions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get interaction by ID")
    public ResponseEntity<DrugInteractionDTO> getInteractionById(@PathVariable Long id) {
        return ResponseEntity.ok(drugInteractionService.getInteractionById(id));
    }

    @GetMapping("/substance/{substanceId}")
    @Operation(summary = "Get all interactions for a specific substance")
    public ResponseEntity<List<DrugInteractionDTO>> getForSubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(drugInteractionService.getInteractionsForSubstance(substanceId));
    }

    @GetMapping("/check")
    @Operation(summary = "Check interaction between two substances")
    public ResponseEntity<List<DrugInteractionDTO>> checkInteraction(
            @RequestParam Long substanceAId, @RequestParam Long substanceBId) {
        return ResponseEntity.ok(drugInteractionService.checkInteractionBetween(substanceAId, substanceBId));
    }

    @PostMapping
    @Operation(summary = "Create a new drug interaction")
    public ResponseEntity<DrugInteractionDTO> createInteraction(@Valid @RequestBody DrugInteractionDTO dto) {
        return new ResponseEntity<>(drugInteractionService.createInteraction(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update drug interaction")
    public ResponseEntity<DrugInteractionDTO> updateInteraction(@PathVariable Long id,
                                                                @Valid @RequestBody DrugInteractionDTO dto) {
        return ResponseEntity.ok(drugInteractionService.updateInteraction(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete drug interaction")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
        drugInteractionService.deleteInteraction(id);
        return ResponseEntity.noContent().build();
    }
}
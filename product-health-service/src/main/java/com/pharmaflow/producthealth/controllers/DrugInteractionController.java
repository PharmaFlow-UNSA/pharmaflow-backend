package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.models.DrugInteraction;
import com.pharmaflow.producthealth.services.DrugInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class DrugInteractionController {

    private final DrugInteractionService drugInteractionService;

    // GET /api/interactions/substance/1
    @GetMapping("/substance/{substanceId}")
    public ResponseEntity<List<DrugInteraction>> getForSubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(drugInteractionService.getInteractionsForSubstance(substanceId));
    }

    // POST /api/interactions/check?productIds=1,2,3
    // Provjera interakcija za listu proizvoda (npr. pri dodavanju u korpu)
    @GetMapping("/check")
    public ResponseEntity<List<DrugInteraction>> checkInteractions(@RequestParam List<Long> productIds) {
        return ResponseEntity.ok(drugInteractionService.checkInteractionsForProducts(productIds));
    }
}
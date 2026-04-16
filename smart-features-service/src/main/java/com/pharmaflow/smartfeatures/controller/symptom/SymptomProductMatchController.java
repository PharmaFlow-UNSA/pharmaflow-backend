package com.pharmaflow.smartfeatures.controller.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.service.symptom.SymptomProductMatchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/symptoms/{symptomId}/matches")
@Tag(name = "Symptoms")
public class SymptomProductMatchController {

    private final SymptomProductMatchService symptomProductMatchService;

    public SymptomProductMatchController(SymptomProductMatchService symptomProductMatchService) {
        this.symptomProductMatchService = symptomProductMatchService;
    }

    @GetMapping
    public ResponseEntity<List<SymptomProductMatchResponseDto>> getMatches(@PathVariable @Positive Long symptomId) {
        return ResponseEntity.ok(symptomProductMatchService.getMatchesBySymptom(symptomId));
    }

    @PostMapping
    public ResponseEntity<SymptomProductMatchResponseDto> createMatch(
            @PathVariable @Positive Long symptomId, @Valid @RequestBody SymptomProductMatchRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(symptomProductMatchService.createMatch(symptomId, requestDto));
    }

    @PutMapping("/{matchId}")
    public ResponseEntity<SymptomProductMatchResponseDto> updateMatch(
            @PathVariable @Positive Long symptomId,
            @PathVariable @Positive Long matchId,
            @Valid @RequestBody SymptomProductMatchRequestDto requestDto) {
        return ResponseEntity.ok(symptomProductMatchService.updateMatch(symptomId, matchId, requestDto));
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<Void> deleteMatch(
            @PathVariable @Positive Long symptomId, @PathVariable @Positive Long matchId) {
        symptomProductMatchService.deleteMatch(symptomId, matchId);
        return ResponseEntity.noContent().build();
    }
}

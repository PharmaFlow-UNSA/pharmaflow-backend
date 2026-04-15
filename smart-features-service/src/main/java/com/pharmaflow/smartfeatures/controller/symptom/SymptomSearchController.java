package com.pharmaflow.smartfeatures.controller.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemResponseDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchResponseDto;
import com.pharmaflow.smartfeatures.service.symptom.SymptomSearchService;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/symptom-searches")
public class SymptomSearchController {

    private final SymptomSearchService symptomSearchService;

    public SymptomSearchController(SymptomSearchService symptomSearchService) {
        this.symptomSearchService = symptomSearchService;
    }

    @GetMapping
    public ResponseEntity<List<SymptomSearchResponseDto>> getSearches(
            @RequestParam(required = false) @NullablePositive Long userId,
            @RequestParam(required = false) @NullablePositive Long patientProfileId) {
        return ResponseEntity.ok(symptomSearchService.getSearches(userId, patientProfileId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SymptomSearchResponseDto> getSearch(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(symptomSearchService.getSearch(id));
    }

    @PostMapping
    public ResponseEntity<SymptomSearchResponseDto> createSearch(
            @Valid @RequestBody SymptomSearchRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(symptomSearchService.createSearch(requestDto));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<SymptomSearchItemResponseDto>> getItems(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(symptomSearchService.getItems(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<SymptomSearchItemResponseDto> addItem(
            @PathVariable @Positive Long id, @Valid @RequestBody SymptomSearchItemRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(symptomSearchService.addItem(id, requestDto));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable @Positive Long id, @PathVariable @Positive Long itemId) {
        symptomSearchService.deleteItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<SymptomProductMatchResponseDto>> getMatches(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(symptomSearchService.getMatches(id));
    }
}

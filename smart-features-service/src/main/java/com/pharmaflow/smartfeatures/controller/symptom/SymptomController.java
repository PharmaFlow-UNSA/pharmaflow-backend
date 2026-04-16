package com.pharmaflow.smartfeatures.controller.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomResponseDto;
import com.pharmaflow.smartfeatures.service.symptom.SymptomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/symptoms")
@Tag(name = "Symptoms")
public class SymptomController {

    private final SymptomService symptomService;

    public SymptomController(SymptomService symptomService) {
        this.symptomService = symptomService;
    }

    @GetMapping
    public ResponseEntity<List<SymptomResponseDto>> getAllSymptoms() {
        return ResponseEntity.ok(symptomService.getAllSymptoms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SymptomResponseDto> getSymptomById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(symptomService.getSymptomById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SymptomResponseDto>> searchSymptoms(
            @RequestParam @NotBlank(message = "Query is required")
                    @Size(min = 2, max = 100, message = "Query must be between 2 and 100 characters")
                    String query) {
        return ResponseEntity.ok(symptomService.searchSymptoms(query));
    }

    @PostMapping
    public ResponseEntity<SymptomResponseDto> createSymptom(@Valid @RequestBody SymptomRequestDto requestDto) {
        SymptomResponseDto createdSymptom = symptomService.createSymptom(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSymptom);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SymptomResponseDto> updateSymptom(
            @PathVariable @Positive Long id, @Valid @RequestBody SymptomRequestDto requestDto) {
        return ResponseEntity.ok(symptomService.updateSymptom(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSymptom(@PathVariable @Positive Long id) {
        symptomService.deleteSymptom(id);
        return ResponseEntity.noContent().build();
    }
}

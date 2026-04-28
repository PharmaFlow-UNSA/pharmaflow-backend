package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.service.TherapyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/therapies")
@Tag(name = "Therapy Management", description = "APIs for managing patient therapies")
public class TherapyController {

    private final TherapyService therapyService;

    public TherapyController(TherapyService therapyService) {
        this.therapyService = therapyService;
    }

    @GetMapping
    @Operation(summary = "Get all therapies", description = "Retrieves therapies with optional pagination, sorting, and filtering. Use ?page=0&size=20&sort=medicationName,asc for pagination. Use ?medicationName=aspirin for filtering.")
    public ResponseEntity<List<TherapyDTO>> getTherapies(
            @PageableDefault(size = 20, sort = "medicationName", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String medicationName,
            @RequestParam(required = false) Integer page) {

        // If filtering by medication name
        if (medicationName != null && !medicationName.isEmpty()) {
            return ResponseEntity.ok(therapyService.findByMedicationNameContaining(medicationName));
        }

        // If pagination is explicitly requested
        if (page != null) {
            return ResponseEntity.ok(therapyService.getTherapiesPaginated(pageable).getContent());
        }

        // Default: return all therapies
        return ResponseEntity.ok(therapyService.getAllTherapies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get therapy by ID", description = "Retrieves a specific therapy by ID")
    public ResponseEntity<TherapyDTO> getTherapyById(@PathVariable Long id) {
        return ResponseEntity.ok(therapyService.getTherapyById(id));
    }

    @PostMapping
    @Operation(summary = "Create a therapy", description = "Creates a new therapy record")
    public ResponseEntity<TherapyDTO> createTherapy(@Valid @RequestBody TherapyDTO therapyDTO) {
        TherapyDTO createdTherapy = therapyService.createTherapy(therapyDTO);
        return new ResponseEntity<>(createdTherapy, HttpStatus.CREATED);
    }

    @PostMapping(params = "bulk")
    @Operation(summary = "Create multiple therapies (bulk)", description = "Bulk creation of therapy records. Use ?bulk=true")
    public ResponseEntity<List<TherapyDTO>> createTherapiesBulk(
            @RequestBody @Valid List<@Valid TherapyDTO> therapyDTOs) {
        List<TherapyDTO> createdTherapies = therapyService.createTherapiesBatch(therapyDTOs);
        return new ResponseEntity<>(createdTherapies, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update therapy", description = "Updates an existing therapy record")
    public ResponseEntity<TherapyDTO> updateTherapy(@PathVariable Long id,
                                                    @Valid @RequestBody TherapyDTO therapyDTO) {
        return ResponseEntity.ok(therapyService.updateTherapy(id, therapyDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update therapy", description = "Applies JSON Patch operations to a therapy")
    public ResponseEntity<TherapyDTO> patchTherapy(@PathVariable Long id,
                                                   @RequestBody String patchDocument) {
        return ResponseEntity.ok(therapyService.patchTherapy(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete therapy", description = "Deletes a therapy record by ID")
    public ResponseEntity<Void> deleteTherapy(@PathVariable Long id) {
        therapyService.deleteTherapy(id);
        return ResponseEntity.noContent().build();
    }
}


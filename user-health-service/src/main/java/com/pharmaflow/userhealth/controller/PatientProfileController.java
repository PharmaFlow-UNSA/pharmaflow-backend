package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.service.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient-profiles")
@Tag(name = "Patient Profile Management", description = "APIs for managing patient health profiles")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    public PatientProfileController(PatientProfileService patientProfileService) {
        this.patientProfileService = patientProfileService;
    }

    @GetMapping
    @Operation(summary = "Get all patient profiles", description = "Retrieves patient profiles with optional pagination, sorting, and filtering. Use ?page=0&size=20&sort=id,asc for pagination. Use ?bloodType=A+ or ?minBMI=18&maxBMI=25 for filtering.")
    public ResponseEntity<List<PatientProfileDTO>> getPatientProfiles(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(required = false) String bloodType,
            @RequestParam(required = false) Double minBMI,
            @RequestParam(required = false) Double maxBMI,
            @RequestParam(required = false) Integer page) {

        // If filtering by blood type
        if (bloodType != null && !bloodType.isEmpty()) {
            return ResponseEntity.ok(patientProfileService.findByBloodType(bloodType));
        }

        // If filtering by BMI range
        if (minBMI != null && maxBMI != null) {
            return ResponseEntity.ok(patientProfileService.findByBMIRange(minBMI, maxBMI));
        }

        // If pagination is explicitly requested
        if (page != null) {
            return ResponseEntity.ok(patientProfileService.getPatientProfilesPaginated(pageable).getContent());
        }

        // Default: return all profiles
        return ResponseEntity.ok(patientProfileService.getAllPatientProfiles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient profile by ID", description = "Retrieves a specific patient profile by ID")
    public ResponseEntity<PatientProfileDTO> getPatientProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(patientProfileService.getPatientProfileById(id));
    }

    @PostMapping
    @Operation(summary = "Create a patient profile", description = "Creates a new patient profile")
    public ResponseEntity<PatientProfileDTO> createPatientProfile(@Valid @RequestBody PatientProfileDTO profileDTO) {
        PatientProfileDTO createdProfile = patientProfileService.createPatientProfile(profileDTO);
        return new ResponseEntity<>(createdProfile, HttpStatus.CREATED);
    }

    @PostMapping(params = "bulk")
    @Operation(summary = "Create multiple patient profiles (bulk)", description = "Bulk creation of patient profiles. Use ?bulk=true")
    public ResponseEntity<List<PatientProfileDTO>> createPatientProfilesBulk(
            @RequestBody @Valid List<@Valid PatientProfileDTO> profileDTOs) {
        List<PatientProfileDTO> createdProfiles = patientProfileService.createPatientProfilesBatch(profileDTOs);
        return new ResponseEntity<>(createdProfiles, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient profile", description = "Updates an existing patient profile")
    public ResponseEntity<PatientProfileDTO> updatePatientProfile(@PathVariable Long id,
                                                                  @Valid @RequestBody PatientProfileDTO profileDTO) {
        return ResponseEntity.ok(patientProfileService.updatePatientProfile(id, profileDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update patient profile", description = "Applies JSON Patch operations to a patient profile")
    public ResponseEntity<PatientProfileDTO> patchPatientProfile(@PathVariable Long id,
                                                                 @RequestBody String patchDocument) {
        return ResponseEntity.ok(patientProfileService.patchPatientProfile(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient profile", description = "Deletes a patient profile by ID")
    public ResponseEntity<Void> deletePatientProfile(@PathVariable Long id) {
        patientProfileService.deletePatientProfile(id);
        return ResponseEntity.noContent().build();
    }
}


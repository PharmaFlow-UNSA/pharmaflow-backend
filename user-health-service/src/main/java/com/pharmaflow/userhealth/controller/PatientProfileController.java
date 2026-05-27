package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.models.enums.BloodType;
import com.pharmaflow.userhealth.service.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(
        summary = "Get all patient profiles",
        description = "Supports pagination (?page=0&size=20&sort=id,asc) and optional filtering by blood type (A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, AB_POSITIVE, AB_NEGATIVE, O_POSITIVE, O_NEGATIVE) or BMI range (?minBMI=18&maxBMI=25)."
    )
    public ResponseEntity<Page<PatientProfileDTO>> getPatientProfiles(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(required = false) BloodType bloodType,
            @RequestParam(required = false) Double minBMI,
            @RequestParam(required = false) Double maxBMI) {
        return ResponseEntity.ok(patientProfileService.findAll(bloodType, minBMI, maxBMI, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient profile by ID", description = "Retrieves a specific patient profile by ID")
    public ResponseEntity<PatientProfileDTO> getPatientProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(patientProfileService.getPatientProfileById(id));
    }

    @PostMapping
    @Operation(summary = "Create a patient profile", description = "Creates a new patient profile")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<PatientProfileDTO> createPatientProfile(@Valid @RequestBody PatientProfileDTO profileDTO) {
        PatientProfileDTO createdProfile = patientProfileService.createPatientProfile(profileDTO);
        return new ResponseEntity<>(createdProfile, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create patient profiles", description = "Creates multiple patient profiles in a single transaction")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<PatientProfileDTO>> createPatientProfilesBatch(
            @RequestBody @Valid List<@Valid PatientProfileDTO> profileDTOs) {
        List<PatientProfileDTO> createdProfiles = patientProfileService.createPatientProfilesBatch(profileDTOs);
        return new ResponseEntity<>(createdProfiles, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient profile", description = "Updates an existing patient profile")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<PatientProfileDTO> updatePatientProfile(@PathVariable Long id,
                                                                  @Valid @RequestBody PatientProfileDTO profileDTO) {
        return ResponseEntity.ok(patientProfileService.updatePatientProfile(id, profileDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update patient profile", description = "Applies JSON Patch operations to a patient profile")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER', 'ADMIN')")
    public ResponseEntity<PatientProfileDTO> patchPatientProfile(@PathVariable Long id,
                                                                 @RequestBody String patchDocument) {
        return ResponseEntity.ok(patientProfileService.patchPatientProfile(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient profile", description = "Deletes a patient profile by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatientProfile(@PathVariable Long id) {
        patientProfileService.deletePatientProfile(id);
        return ResponseEntity.noContent().build();
    }
}


package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.service.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    @Operation(summary = "Get all patient profiles", description = "Retrieves a list of all patient profiles")
    public ResponseEntity<List<PatientProfileDTO>> getAllPatientProfiles() {
        return ResponseEntity.ok(patientProfileService.getAllPatientProfiles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient profile by ID", description = "Retrieves a specific patient profile by ID")
    public ResponseEntity<PatientProfileDTO> getPatientProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(patientProfileService.getPatientProfileById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new patient profile", description = "Creates a new patient profile")
    public ResponseEntity<PatientProfileDTO> createPatientProfile(@Valid @RequestBody PatientProfileDTO profileDTO) {
        PatientProfileDTO createdProfile = patientProfileService.createPatientProfile(profileDTO);
        return new ResponseEntity<>(createdProfile, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient profile", description = "Updates an existing patient profile")
    public ResponseEntity<PatientProfileDTO> updatePatientProfile(@PathVariable Long id,
                                                                  @Valid @RequestBody PatientProfileDTO profileDTO) {
        return ResponseEntity.ok(patientProfileService.updatePatientProfile(id, profileDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient profile", description = "Deletes a patient profile by ID")
    public ResponseEntity<Void> deletePatientProfile(@PathVariable Long id) {
        patientProfileService.deletePatientProfile(id);
        return ResponseEntity.noContent().build();
    }
}


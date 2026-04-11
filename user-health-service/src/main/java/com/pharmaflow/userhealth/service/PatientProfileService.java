package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.repositories.PatientProfileRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final ModelMapper modelMapper;

    public PatientProfileService(PatientProfileRepository patientProfileRepository, ModelMapper modelMapper) {
        this.patientProfileRepository = patientProfileRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<PatientProfileDTO> getAllPatientProfiles() {
        return patientProfileRepository.findAll().stream()
                .map(profile -> modelMapper.map(profile, PatientProfileDTO.class))
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientProfileDTO getPatientProfileById(Long id) {
        PatientProfile profile = patientProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with id: " + id));
        return modelMapper.map(profile, PatientProfileDTO.class);
    }

    @Transactional
    public PatientProfileDTO createPatientProfile(PatientProfileDTO profileDTO) {
        PatientProfile profile = modelMapper.map(profileDTO, PatientProfile.class);
        PatientProfile savedProfile = patientProfileRepository.save(profile);
        return modelMapper.map(savedProfile, PatientProfileDTO.class);
    }

    @Transactional
    public PatientProfileDTO updatePatientProfile(Long id, PatientProfileDTO profileDTO) {
        PatientProfile profile = patientProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with id: " + id));

        modelMapper.map(profileDTO, profile);
        profile.setId(id); // Ensure ID is not changed
        
        PatientProfile updatedProfile = patientProfileRepository.save(profile);
        return modelMapper.map(updatedProfile, PatientProfileDTO.class);
    }

    @Transactional
    public void deletePatientProfile(Long id) {
        if (!patientProfileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient profile not found with id: " + id);
        }
        patientProfileRepository.deleteById(id);
    }
}


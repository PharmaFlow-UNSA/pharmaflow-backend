package com.pharmaflow.userhealth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.repositories.PatientProfileRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PatientProfileService {

    private static final Logger log = LoggerFactory.getLogger(PatientProfileService.class);

    private final PatientProfileRepository patientProfileRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public PatientProfileService(PatientProfileRepository patientProfileRepository,
                               ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.patientProfileRepository = patientProfileRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PatientProfileDTO> getAllPatientProfiles() {
        long startTime = System.currentTimeMillis();
        List<PatientProfileDTO> profiles = patientProfileRepository.findAll().stream()
                .map(profile -> modelMapper.map(profile, PatientProfileDTO.class))
                .toList();
        log.info("getAllPatientProfiles executed in {} ms", System.currentTimeMillis() - startTime);
        return profiles;
    }

    @Transactional(readOnly = true)
    public Page<PatientProfileDTO> getPatientProfilesPaginated(Pageable pageable) {
        long startTime = System.currentTimeMillis();
        Page<PatientProfileDTO> profiles = patientProfileRepository.findAll(pageable)
                .map(profile -> modelMapper.map(profile, PatientProfileDTO.class));
        log.info("getPatientProfilesPaginated executed in {} ms, returned {} of {} total profiles",
                System.currentTimeMillis() - startTime, profiles.getNumberOfElements(), profiles.getTotalElements());
        return profiles;
    }

    @Transactional(readOnly = true)
    public PatientProfileDTO getPatientProfileById(Long id) {
        PatientProfile profile = patientProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with id: " + id));
        return modelMapper.map(profile, PatientProfileDTO.class);
    }

    @Transactional(readOnly = true)
    public List<PatientProfileDTO> findByBloodType(String bloodType) {
        long startTime = System.currentTimeMillis();
        List<PatientProfileDTO> profiles = patientProfileRepository.findByBloodType(bloodType).stream()
                .map(profile -> modelMapper.map(profile, PatientProfileDTO.class))
                .toList();
        log.info("findByBloodType executed in {} ms, found {} profiles",
                System.currentTimeMillis() - startTime, profiles.size());
        return profiles;
    }

    @Transactional(readOnly = true)
    public List<PatientProfileDTO> findByBMIRange(Double minBMI, Double maxBMI) {
        long startTime = System.currentTimeMillis();
        List<PatientProfileDTO> profiles = patientProfileRepository.findByBMIRange(minBMI, maxBMI).stream()
                .map(profile -> modelMapper.map(profile, PatientProfileDTO.class))
                .toList();
        log.info("findByBMIRange executed in {} ms, found {} profiles",
                System.currentTimeMillis() - startTime, profiles.size());
        return profiles;
    }

    @Transactional
    public PatientProfileDTO createPatientProfile(PatientProfileDTO profileDTO) {
        PatientProfile profile = modelMapper.map(profileDTO, PatientProfile.class);
        PatientProfile savedProfile = patientProfileRepository.save(profile);
        log.info("PatientProfile created with id: {}", savedProfile.getId());
        return modelMapper.map(savedProfile, PatientProfileDTO.class);
    }

    @Transactional
    public List<PatientProfileDTO> createPatientProfilesBatch(List<PatientProfileDTO> profileDTOs) {
        long startTime = System.currentTimeMillis();
        List<PatientProfile> profiles = new ArrayList<>();

        for (PatientProfileDTO dto : profileDTOs) {
            PatientProfile profile = modelMapper.map(dto, PatientProfile.class);
            profiles.add(profile);
        }

        List<PatientProfile> savedProfiles = patientProfileRepository.saveAll(profiles);
        log.info("Batch created {} patient profiles in {} ms", savedProfiles.size(), System.currentTimeMillis() - startTime);

        return savedProfiles.stream()
                .map(profile -> modelMapper.map(profile, PatientProfileDTO.class))
                .toList();
    }

    @Transactional
    public PatientProfileDTO updatePatientProfile(Long id, PatientProfileDTO profileDTO) {
        PatientProfile profile = patientProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with id: " + id));

        modelMapper.map(profileDTO, profile);
        profile.setId(id);

        PatientProfile updatedProfile = patientProfileRepository.save(profile);
        log.info("PatientProfile updated with id: {}", updatedProfile.getId());
        return modelMapper.map(updatedProfile, PatientProfileDTO.class);
    }

    @Transactional
    public PatientProfileDTO patchPatientProfile(Long id, String patchDocument) {
        try {
            PatientProfile profile = patientProfileRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with id: " + id));

            JsonNode profileJson = objectMapper.valueToTree(profile);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedProfileJson = patch.apply(profileJson);
            PatientProfile patchedProfile = objectMapper.treeToValue(patchedProfileJson, PatientProfile.class);

            PatientProfile savedProfile = patientProfileRepository.save(patchedProfile);
            log.info("PatientProfile patched with id: {}", savedProfile.getId());
            return modelMapper.map(savedProfile, PatientProfileDTO.class);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new RuntimeException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deletePatientProfile(Long id) {
        if (!patientProfileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient profile not found with id: " + id);
        }
        patientProfileRepository.deleteById(id);
        log.info("PatientProfile deleted with id: {}", id);
    }
}


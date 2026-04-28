package com.pharmaflow.userhealth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.Allergy;
import com.pharmaflow.userhealth.repositories.AllergyRepository;
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
public class AllergyService {

    private static final Logger log = LoggerFactory.getLogger(AllergyService.class);

    private final AllergyRepository allergyRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public AllergyService(AllergyRepository allergyRepository, ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.allergyRepository = allergyRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AllergyDTO> getAllAllergies() {
        long startTime = System.currentTimeMillis();
        List<AllergyDTO> allergies = allergyRepository.findAll().stream()
                .map(allergy -> modelMapper.map(allergy, AllergyDTO.class))
                .toList();
        log.info("getAllAllergies executed in {} ms", System.currentTimeMillis() - startTime);
        return allergies;
    }

    @Transactional(readOnly = true)
    public Page<AllergyDTO> getAllergiesPaginated(Pageable pageable) {
        long startTime = System.currentTimeMillis();
        Page<AllergyDTO> allergies = allergyRepository.findAll(pageable)
                .map(allergy -> modelMapper.map(allergy, AllergyDTO.class));
        log.info("getAllergiesPaginated executed in {} ms, returned {} of {} total allergies",
                System.currentTimeMillis() - startTime, allergies.getNumberOfElements(), allergies.getTotalElements());
        return allergies;
    }

    @Transactional(readOnly = true)
    public AllergyDTO getAllergyById(Long id) {
        Allergy allergy = allergyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found with id: " + id));
        return modelMapper.map(allergy, AllergyDTO.class);
    }

    @Transactional
    public AllergyDTO createAllergy(AllergyDTO allergyDTO) {
        Allergy allergy = modelMapper.map(allergyDTO, Allergy.class);
        Allergy savedAllergy = allergyRepository.save(allergy);
        return modelMapper.map(savedAllergy, AllergyDTO.class);
    }

    @Transactional
    public AllergyDTO updateAllergy(Long id, AllergyDTO allergyDTO) {
        Allergy allergy = allergyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found with id: " + id));

        allergy.setAllergen(allergyDTO.getAllergen());
        allergy.setSeverity(allergyDTO.getSeverity());
        allergy.setActiveSubstance(allergyDTO.getActiveSubstance());
        
        Allergy updatedAllergy = allergyRepository.save(allergy);
        return modelMapper.map(updatedAllergy, AllergyDTO.class);
    }

    @Transactional
    public void deleteAllergy(Long id) {
        if (!allergyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Allergy not found with id: " + id);
        }
        allergyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AllergyDTO> findBySeverity(String severity) {
        long startTime = System.currentTimeMillis();
        List<AllergyDTO> allergies = allergyRepository.findBySeverity(severity).stream()
                .map(allergy -> modelMapper.map(allergy, AllergyDTO.class))
                .toList();
        log.info("findBySeverity executed in {} ms, found {} allergies",
                System.currentTimeMillis() - startTime, allergies.size());
        return allergies;
    }

    @Transactional(readOnly = true)
    public List<AllergyDTO> findByAllergenContaining(String name) {
        long startTime = System.currentTimeMillis();
        List<AllergyDTO> allergies = allergyRepository.findByAllergenContainingIgnoreCase(name).stream()
                .map(allergy -> modelMapper.map(allergy, AllergyDTO.class))
                .toList();
        log.info("findByAllergenContaining executed in {} ms, found {} allergies",
                System.currentTimeMillis() - startTime, allergies.size());
        return allergies;
    }

    @Transactional
    public List<AllergyDTO> createAllergiesBatch(List<AllergyDTO> allergyDTOs) {
        long startTime = System.currentTimeMillis();
        List<Allergy> allergies = new ArrayList<>();

        for (AllergyDTO dto : allergyDTOs) {
            Allergy allergy = modelMapper.map(dto, Allergy.class);
            allergies.add(allergy);
        }

        List<Allergy> savedAllergies = allergyRepository.saveAll(allergies);
        log.info("Batch created {} allergies in {} ms", savedAllergies.size(), System.currentTimeMillis() - startTime);

        return savedAllergies.stream()
                .map(allergy -> modelMapper.map(allergy, AllergyDTO.class))
                .toList();
    }

    @Transactional
    public AllergyDTO patchAllergy(Long id, String patchDocument) {
        try {
            Allergy allergy = allergyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Allergy not found with id: " + id));

            JsonNode allergyJson = objectMapper.valueToTree(allergy);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedAllergyJson = patch.apply(allergyJson);
            Allergy patchedAllergy = objectMapper.treeToValue(patchedAllergyJson, Allergy.class);

            Allergy savedAllergy = allergyRepository.save(patchedAllergy);
            log.info("Allergy patched with id: {}", savedAllergy.getId());
            return modelMapper.map(savedAllergy, AllergyDTO.class);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new RuntimeException("Error applying patch: " + e.getMessage(), e);
        }
    }
}

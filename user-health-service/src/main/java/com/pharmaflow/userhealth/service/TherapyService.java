package com.pharmaflow.userhealth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.exception.PatchOperationException;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.Therapy;
import com.pharmaflow.userhealth.repositories.TherapyRepository;
import com.pharmaflow.userhealth.specifications.TherapySpecs;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TherapyService {

    private static final Logger log = LoggerFactory.getLogger(TherapyService.class);

    private final TherapyRepository therapyRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public TherapyService(TherapyRepository therapyRepository, ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.therapyRepository = therapyRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TherapyDTO> getAllTherapies() {
        long startTime = System.currentTimeMillis();
        List<TherapyDTO> therapies = therapyRepository.findAll().stream()
                .map(therapy -> modelMapper.map(therapy, TherapyDTO.class))
                .toList();
        log.info("getAllTherapies executed in {} ms", System.currentTimeMillis() - startTime);
        return therapies;
    }

    @Transactional(readOnly = true)
    public Page<TherapyDTO> getTherapiesPaginated(Pageable pageable) {
        long startTime = System.currentTimeMillis();
        Page<TherapyDTO> therapies = therapyRepository.findAll(pageable)
                .map(therapy -> modelMapper.map(therapy, TherapyDTO.class));
        log.info("getTherapiesPaginated executed in {} ms, returned {} of {} total therapies",
                System.currentTimeMillis() - startTime, therapies.getNumberOfElements(), therapies.getTotalElements());
        return therapies;
    }

    @Transactional(readOnly = true)
    public TherapyDTO getTherapyById(Long id) {
        Therapy therapy = therapyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy not found with id: " + id));
        return modelMapper.map(therapy, TherapyDTO.class);
    }


    @Transactional
    public TherapyDTO createTherapy(TherapyDTO therapyDTO) {
        Therapy therapy = modelMapper.map(therapyDTO, Therapy.class);
        Therapy savedTherapy = therapyRepository.save(therapy);
        log.info("Therapy created with id: {}", savedTherapy.getId());
        return modelMapper.map(savedTherapy, TherapyDTO.class);
    }

    @Transactional
    public List<TherapyDTO> createTherapiesBatch(List<TherapyDTO> therapyDTOs) {
        long startTime = System.currentTimeMillis();
        List<Therapy> therapies = new ArrayList<>();

        for (TherapyDTO dto : therapyDTOs) {
            Therapy therapy = modelMapper.map(dto, Therapy.class);
            therapies.add(therapy);
        }

        List<Therapy> savedTherapies = therapyRepository.saveAll(therapies);
        log.info("Batch created {} therapies in {} ms", savedTherapies.size(), System.currentTimeMillis() - startTime);

        return savedTherapies.stream()
                .map(therapy -> modelMapper.map(therapy, TherapyDTO.class))
                .toList();
    }

    @Transactional
    public TherapyDTO updateTherapy(Long id, TherapyDTO therapyDTO) {
        Therapy therapy = therapyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy not found with id: " + id));

        therapy.setMedicationName(therapyDTO.getMedicationName());
        therapy.setDosage(therapyDTO.getDosage());
        therapy.setFrequency(therapyDTO.getFrequency());

        Therapy updatedTherapy = therapyRepository.save(therapy);
        log.info("Therapy updated with id: {}", updatedTherapy.getId());
        return modelMapper.map(updatedTherapy, TherapyDTO.class);
    }

    @Transactional
    public TherapyDTO patchTherapy(Long id, String patchDocument) {
        try {
            Therapy therapy = therapyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Therapy not found with id: " + id));

            JsonNode therapyJson = objectMapper.valueToTree(therapy);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedTherapyJson = patch.apply(therapyJson);
            Therapy patchedTherapy = objectMapper.treeToValue(patchedTherapyJson, Therapy.class);

            Therapy savedTherapy = therapyRepository.save(patchedTherapy);
            log.info("Therapy patched with id: {}", savedTherapy.getId());
            return modelMapper.map(savedTherapy, TherapyDTO.class);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteTherapy(Long id) {
        if (!therapyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Therapy not found with id: " + id);
        }
        therapyRepository.deleteById(id);
        log.info("Therapy deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<TherapyDTO> findAll(String medicationName, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Therapy> spec = Specification
                .where(TherapySpecs.medicationNameContains(medicationName));

        Page<TherapyDTO> result = therapyRepository.findAll(spec, pageable)
                .map(therapy -> modelMapper.map(therapy, TherapyDTO.class));

        log.info("findAll executed in {} ms, returned {} of {} total therapies",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }
}

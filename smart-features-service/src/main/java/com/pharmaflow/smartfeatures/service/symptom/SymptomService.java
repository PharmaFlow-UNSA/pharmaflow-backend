package com.pharmaflow.smartfeatures.service.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomResponseDto;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.symptom.SymptomMapper;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import com.pharmaflow.smartfeatures.util.SymptomTextNormalizer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SymptomService {

    private final SymptomRepository symptomRepository;
    private final SymptomMapper symptomMapper;

    public SymptomService(SymptomRepository symptomRepository, SymptomMapper symptomMapper) {
        this.symptomRepository = symptomRepository;
        this.symptomMapper = symptomMapper;
    }

    @Transactional(readOnly = true)
    public List<SymptomResponseDto> getAllSymptoms() {
        return symptomRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(symptomMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SymptomResponseDto getSymptomById(Long id) {
        return symptomMapper.toResponseDto(findActiveSymptomById(id));
    }

    @Transactional(readOnly = true)
    public List<SymptomResponseDto> searchSymptoms(String query) {
        String sanitizedQuery = sanitizeQuery(query);
        return symptomRepository.searchActiveSymptoms(sanitizedQuery).stream()
                .map(symptomMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public SymptomResponseDto createSymptom(SymptomRequestDto requestDto) {
        SymptomRequestDto sanitizedRequest = sanitizeRequest(requestDto);
        ensureNameIsUnique(sanitizedRequest.getName(), null);

        Symptom symptom = symptomMapper.toEntity(sanitizedRequest);
        Symptom savedSymptom = symptomRepository.save(symptom);
        return symptomMapper.toResponseDto(savedSymptom);
    }

    @Transactional
    public SymptomResponseDto updateSymptom(Long id, SymptomRequestDto requestDto) {
        SymptomRequestDto sanitizedRequest = sanitizeRequest(requestDto);
        Symptom existingSymptom = findUpdatableSymptomById(id, sanitizedRequest);
        ensureNameIsUnique(sanitizedRequest.getName(), id);

        symptomMapper.updateEntity(sanitizedRequest, existingSymptom);
        Symptom updatedSymptom = symptomRepository.save(existingSymptom);
        return symptomMapper.toResponseDto(updatedSymptom);
    }

    @Transactional
    public void deleteSymptom(Long id) {
        Symptom symptom = findActiveSymptomById(id);
        symptomRepository.delete(symptom);
    }

    private Symptom findSymptomById(Long id) {
        return symptomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Symptom not found with id: " + id));
    }

    private Symptom findActiveSymptomById(Long id) {
        Symptom symptom = findSymptomById(id);
        if (!symptom.isActive()) {
            throw new ResourceNotFoundException("Symptom not found with id: " + id);
        }
        return symptom;
    }

    private Symptom findUpdatableSymptomById(Long id, SymptomRequestDto requestDto) {
        Symptom symptom = findSymptomById(id);
        if (!symptom.isActive() && !Boolean.TRUE.equals(requestDto.getActive())) {
            throw new ResourceNotFoundException("Symptom not found with id: " + id);
        }
        return symptom;
    }

    private void ensureNameIsUnique(String name, Long currentSymptomId) {
        String normalizedName = normalizeName(name);
        boolean duplicateExists = currentSymptomId == null
                ? symptomRepository.existsByNormalizedName(normalizedName)
                : symptomRepository.existsByNormalizedNameAndSymptomIdNot(normalizedName, currentSymptomId);

        if (duplicateExists) {
            throw new DuplicateResourceException("Symptom with name '" + name + "' already exists.");
        }
    }

    private SymptomRequestDto sanitizeRequest(SymptomRequestDto requestDto) {
        String sanitizedName = SymptomTextNormalizer.sanitizeName(requestDto.getName());
        if (sanitizedName == null || sanitizedName.length() < 2 || sanitizedName.length() > 100) {
            throw new BadRequestException("Symptom name must be between 2 and 100 characters after trimming.");
        }

        return new SymptomRequestDto(
                sanitizedName,
                SymptomTextNormalizer.sanitizeDescription(requestDto.getDescription()),
                requestDto.getSeverityLevel(),
                requestDto.getActive());
    }

    private String sanitizeQuery(String query) {
        String sanitizedQuery = SymptomTextNormalizer.sanitizeQuery(query);
        if (sanitizedQuery == null || sanitizedQuery.length() < 2) {
            throw new BadRequestException("Search query must contain at least 2 characters.");
        }
        return sanitizedQuery;
    }

    private String normalizeName(String name) {
        return SymptomTextNormalizer.normalizeName(name);
    }
}

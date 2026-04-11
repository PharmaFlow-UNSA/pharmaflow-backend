package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.Allergy;
import com.pharmaflow.userhealth.repositories.AllergyRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AllergyService {

    private final AllergyRepository allergyRepository;
    private final ModelMapper modelMapper;

    public AllergyService(AllergyRepository allergyRepository, ModelMapper modelMapper) {
        this.allergyRepository = allergyRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<AllergyDTO> getAllAllergies() {
        return allergyRepository.findAll().stream()
                .map(allergy -> modelMapper.map(allergy, AllergyDTO.class))
                .toList();
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
}


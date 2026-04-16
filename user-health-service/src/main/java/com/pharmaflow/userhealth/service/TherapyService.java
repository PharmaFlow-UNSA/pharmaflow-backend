package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.Therapy;
import com.pharmaflow.userhealth.repositories.TherapyRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TherapyService {

    private final TherapyRepository therapyRepository;
    private final ModelMapper modelMapper;

    public TherapyService(TherapyRepository therapyRepository, ModelMapper modelMapper) {
        this.therapyRepository = therapyRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<TherapyDTO> getAllTherapies() {
        return therapyRepository.findAll().stream()
                .map(therapy -> modelMapper.map(therapy, TherapyDTO.class))
                .toList();
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
        return modelMapper.map(savedTherapy, TherapyDTO.class);
    }

    @Transactional
    public TherapyDTO updateTherapy(Long id, TherapyDTO therapyDTO) {
        Therapy therapy = therapyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy not found with id: " + id));

        therapy.setMedicationName(therapyDTO.getMedicationName());
        therapy.setDosage(therapyDTO.getDosage());
        therapy.setFrequency(therapyDTO.getFrequency());

        Therapy updatedTherapy = therapyRepository.save(therapy);
        return modelMapper.map(updatedTherapy, TherapyDTO.class);
    }

    @Transactional
    public void deleteTherapy(Long id) {
        if (!therapyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Therapy not found with id: " + id);
        }
        therapyRepository.deleteById(id);
    }
}


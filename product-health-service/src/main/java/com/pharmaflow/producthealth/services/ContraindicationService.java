package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.dto.ContraindicationDTO;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.Contraindication;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.ContraindicationRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContraindicationService {

    private final ContraindicationRepository contraindicationRepository;
    private final SubstanceRepository substanceRepository;

    @Transactional(readOnly = true)
    public List<ContraindicationDTO> getAllContraindications() {
        return contraindicationRepository.findAllWithSubstance().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ContraindicationDTO getContraindicationById(Long id) {
        return toDTO(contraindicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contraindication with ID " + id + " not found.")));
    }

    @Transactional(readOnly = true)
    public List<ContraindicationDTO> getBySubstance(Long substanceId) {
        if (!substanceRepository.existsById(substanceId))
            throw new ResourceNotFoundException("Substance with ID " + substanceId + " not found.");
        return contraindicationRepository.findBySubstanceIdWithDetails(substanceId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public ContraindicationDTO createContraindication(ContraindicationDTO dto) {
        Substance substance = substanceRepository.findById(dto.getSubstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Substance with ID " + dto.getSubstanceId() + " not found."));
        Contraindication c = new Contraindication();
        c.setSubstance(substance);
        c.setType(Contraindication.ContraindicationType.valueOf(dto.getType()));
        c.setConditionName(dto.getConditionName());
        c.setDescription(dto.getDescription());
        c.setSeverityType(Contraindication.SeverityType.valueOf(dto.getSeverityType()));
        return toDTO(contraindicationRepository.save(c));
    }

    @Transactional
    public ContraindicationDTO updateContraindication(Long id, ContraindicationDTO dto) {
        Contraindication c = contraindicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contraindication with ID " + id + " not found."));
        Substance substance = substanceRepository.findById(dto.getSubstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Substance with ID " + dto.getSubstanceId() + " not found."));
        c.setSubstance(substance);
        c.setType(Contraindication.ContraindicationType.valueOf(dto.getType()));
        c.setConditionName(dto.getConditionName());
        c.setDescription(dto.getDescription());
        c.setSeverityType(Contraindication.SeverityType.valueOf(dto.getSeverityType()));
        return toDTO(contraindicationRepository.save(c));
    }

    @Transactional
    public void deleteContraindication(Long id) {
        if (!contraindicationRepository.existsById(id))
            throw new ResourceNotFoundException("Contraindication with ID " + id + " not found.");
        contraindicationRepository.deleteById(id);
    }

    private ContraindicationDTO toDTO(Contraindication c) {
        ContraindicationDTO dto = new ContraindicationDTO();
        dto.setId(c.getId());
        dto.setSubstanceId(c.getSubstance().getId());
        dto.setSubstanceName(c.getSubstance().getCommonName());
        dto.setType(c.getType().name());
        dto.setConditionName(c.getConditionName());
        dto.setDescription(c.getDescription());
        dto.setSeverityType(c.getSeverityType().name());
        return dto;
    }
}

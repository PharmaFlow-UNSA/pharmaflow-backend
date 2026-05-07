package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.dto.DrugInteractionDTO;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.DrugInteraction;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.DrugInteractionRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final DrugInteractionRepository interactionRepository;
    private final SubstanceRepository substanceRepository;

    @Transactional(readOnly = true)
    public List<DrugInteractionDTO> getAllInteractions() {
        return interactionRepository.findAllWithSubstances().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DrugInteractionDTO getInteractionById(Long id) {
        return toDTO(interactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drug interaction with ID " + id + " not found.")));
    }

    @Transactional(readOnly = true)
    public List<DrugInteractionDTO> getInteractionsForSubstance(Long substanceId) {
        if (!substanceRepository.existsById(substanceId))
            throw new ResourceNotFoundException("Substance with ID " + substanceId + " not found.");
        return interactionRepository.findAllBySubstanceId(substanceId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<DrugInteractionDTO> checkInteractionBetween(Long idA, Long idB) {
        return interactionRepository.findInteractionBetween(idA, idB).stream().map(this::toDTO).toList();
    }

    @Transactional
    public DrugInteractionDTO createInteraction(DrugInteractionDTO dto) {
        if (interactionRepository.existsBySubstanceAIdAndSubstanceBId(dto.getSubstanceAId(), dto.getSubstanceBId()) ||
            interactionRepository.existsBySubstanceAIdAndSubstanceBId(dto.getSubstanceBId(), dto.getSubstanceAId()))
            throw new DuplicateResourceException("Interaction between substances " + dto.getSubstanceAId() + " and " + dto.getSubstanceBId() + " already exists.");

        if (dto.getSubstanceAId().equals(dto.getSubstanceBId()))
            throw new IllegalArgumentException("A substance cannot interact with itself.");

        Substance substanceA = substanceRepository.findById(dto.getSubstanceAId())
                .orElseThrow(() -> new ResourceNotFoundException("Substance with ID " + dto.getSubstanceAId() + " not found."));
        Substance substanceB = substanceRepository.findById(dto.getSubstanceBId())
                .orElseThrow(() -> new ResourceNotFoundException("Substance with ID " + dto.getSubstanceBId() + " not found."));

        DrugInteraction interaction = new DrugInteraction();
        interaction.setSubstanceA(substanceA);
        interaction.setSubstanceB(substanceB);
        interaction.setSeverity(DrugInteraction.SeverityLevel.valueOf(dto.getSeverity()));
        interaction.setDescription(dto.getDescription());
        interaction.setClinicalRecommendation(dto.getClinicalRecommendation());
        return toDTO(interactionRepository.save(interaction));
    }

    @Transactional
    public DrugInteractionDTO updateInteraction(Long id, DrugInteractionDTO dto) {
        DrugInteraction interaction = interactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drug interaction with ID " + id + " not found."));
        interaction.setSeverity(DrugInteraction.SeverityLevel.valueOf(dto.getSeverity()));
        interaction.setDescription(dto.getDescription());
        interaction.setClinicalRecommendation(dto.getClinicalRecommendation());
        return toDTO(interactionRepository.save(interaction));
    }

    @Transactional
    public void deleteInteraction(Long id) {
        if (!interactionRepository.existsById(id))
            throw new ResourceNotFoundException("Drug interaction with ID " + id + " not found.");
        interactionRepository.deleteById(id);
    }

    private DrugInteractionDTO toDTO(DrugInteraction i) {
        DrugInteractionDTO dto = new DrugInteractionDTO();
        dto.setId(i.getId());
        dto.setSubstanceAId(i.getSubstanceA().getId());
        dto.setSubstanceBId(i.getSubstanceB().getId());
        dto.setSubstanceAName(i.getSubstanceA().getCommonName());
        dto.setSubstanceBName(i.getSubstanceB().getCommonName());
        dto.setSeverity(i.getSeverity().name());
        dto.setDescription(i.getDescription());
        dto.setClinicalRecommendation(i.getClinicalRecommendation());
        return dto;
    }
}

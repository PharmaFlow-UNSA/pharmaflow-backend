package com.pharmaflow.producthealth.service;

import com.pharmaflow.producthealth.dto.DrugInteractionDTO;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.DrugInteraction;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.DrugInteractionRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import com.pharmaflow.producthealth.services.DrugInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrugInteractionServiceTest {

    @Mock private DrugInteractionRepository interactionRepository;
    @Mock private SubstanceRepository substanceRepository;
    @InjectMocks private DrugInteractionService drugInteractionService;

    private Substance substanceA;
    private Substance substanceB;
    private DrugInteraction testInteraction;
    private DrugInteractionDTO validCreateDTO;

    @BeforeEach
    void setUp() {
        substanceA = new Substance();
        substanceA.setId(1L);
        substanceA.setInn("ibuprofen");
        substanceA.setCommonName("Ibuprofen");

        substanceB = new Substance();
        substanceB.setId(2L);
        substanceB.setInn("warfarin");
        substanceB.setCommonName("Varfarin");

        testInteraction = new DrugInteraction();
        testInteraction.setId(1L);
        testInteraction.setSubstanceA(substanceA);
        testInteraction.setSubstanceB(substanceB);
        testInteraction.setSeverity(DrugInteraction.SeverityLevel.MAJOR);
        testInteraction.setDescription("Pojačano krvarenje");
        testInteraction.setClinicalRecommendation("Izbjegavati kombinaciju");

        validCreateDTO = new DrugInteractionDTO();
        validCreateDTO.setSubstanceAId(1L);
        validCreateDTO.setSubstanceBId(2L);
        validCreateDTO.setSeverity("MAJOR");
        validCreateDTO.setDescription("Opis interakcije koji je dovoljno dugačak");
    }

    @Test
    void getAllInteractions_shouldReturnList() {
        when(interactionRepository.findAllWithSubstances()).thenReturn(List.of(testInteraction));

        List<DrugInteractionDTO> result = drugInteractionService.getAllInteractions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MAJOR", result.get(0).getSeverity());
        assertEquals("Ibuprofen", result.get(0).getSubstanceAName());
        verify(interactionRepository, times(1)).findAllWithSubstances();
    }

    @Test
    void getInteractionById_whenExists_shouldReturnDTO() {
        when(interactionRepository.findById(1L)).thenReturn(Optional.of(testInteraction));

        DrugInteractionDTO result = drugInteractionService.getInteractionById(1L);

        assertNotNull(result);
        assertEquals("MAJOR", result.getSeverity());
        assertEquals(1L, result.getSubstanceAId());
    }

    @Test
    void getInteractionById_whenNotExists_shouldThrowException() {
        when(interactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> drugInteractionService.getInteractionById(999L));
    }

    @Test
    void createInteraction_withValidData_shouldReturnDTO() {
        when(interactionRepository.existsBySubstanceAIdAndSubstanceBId(1L, 2L)).thenReturn(false);
        when(interactionRepository.existsBySubstanceAIdAndSubstanceBId(2L, 1L)).thenReturn(false);
        when(substanceRepository.findById(1L)).thenReturn(Optional.of(substanceA));
        when(substanceRepository.findById(2L)).thenReturn(Optional.of(substanceB));
        when(interactionRepository.save(any(DrugInteraction.class))).thenReturn(testInteraction);

        DrugInteractionDTO result = drugInteractionService.createInteraction(validCreateDTO);

        assertNotNull(result);
        assertEquals("MAJOR", result.getSeverity());
        verify(interactionRepository, times(1)).save(any(DrugInteraction.class));
    }

    @Test
    void createInteraction_whenAlreadyExists_shouldThrowDuplicateException() {
        when(interactionRepository.existsBySubstanceAIdAndSubstanceBId(1L, 2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> drugInteractionService.createInteraction(validCreateDTO));
        verify(interactionRepository, never()).save(any());
    }

    @Test
    void createInteraction_withSameSubstance_shouldThrowIllegalArgument() {
        validCreateDTO.setSubstanceBId(1L); // same as A

        assertThrows(IllegalArgumentException.class,
                () -> drugInteractionService.createInteraction(validCreateDTO));
        verify(interactionRepository, never()).save(any());
    }

    @Test
    void createInteraction_withNonexistentSubstance_shouldThrowException() {
        when(interactionRepository.existsBySubstanceAIdAndSubstanceBId(anyLong(), anyLong())).thenReturn(false);
        when(substanceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> drugInteractionService.createInteraction(validCreateDTO));
        verify(interactionRepository, never()).save(any());
    }

    @Test
    void deleteInteraction_whenExists_shouldDelete() {
        when(interactionRepository.existsById(1L)).thenReturn(true);

        drugInteractionService.deleteInteraction(1L);

        verify(interactionRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteInteraction_whenNotExists_shouldThrowException() {
        when(interactionRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> drugInteractionService.deleteInteraction(999L));
        verify(interactionRepository, never()).deleteById(any());
    }
}

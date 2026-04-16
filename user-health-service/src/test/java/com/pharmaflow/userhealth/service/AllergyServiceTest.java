package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.Allergy;
import com.pharmaflow.userhealth.repositories.AllergyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllergyServiceTest {

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AllergyService allergyService;

    private Allergy testAllergy;
    private AllergyDTO testAllergyDTO;

    @BeforeEach
    void setUp() {
        testAllergy = new Allergy();
        testAllergy.setId(1L);
        testAllergy.setAllergen("Penicillin");
        testAllergy.setSeverity("High");
        testAllergy.setActiveSubstance("Penicillin G");

        testAllergyDTO = new AllergyDTO();
        testAllergyDTO.setId(1L);
        testAllergyDTO.setAllergen("Penicillin");
        testAllergyDTO.setSeverity("High");
        testAllergyDTO.setActiveSubstance("Penicillin G");
    }

    @Test
    void getAllAllergies_ShouldReturnListOfAllergies() {
        // Arrange
        when(allergyRepository.findAll()).thenReturn(Arrays.asList(testAllergy));
        when(modelMapper.map(any(Allergy.class), eq(AllergyDTO.class))).thenReturn(testAllergyDTO);

        // Act
        List<AllergyDTO> result = allergyService.getAllAllergies();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(allergyRepository, times(1)).findAll();
    }

    @Test
    void getAllergyById_WhenAllergyExists_ShouldReturnAllergy() {
        // Arrange
        when(allergyRepository.findById(1L)).thenReturn(Optional.of(testAllergy));
        when(modelMapper.map(any(Allergy.class), eq(AllergyDTO.class))).thenReturn(testAllergyDTO);

        // Act
        AllergyDTO result = allergyService.getAllergyById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Penicillin", result.getAllergen());
        verify(allergyRepository, times(1)).findById(1L);
    }

    @Test
    void getAllergyById_WhenAllergyNotExists_ShouldThrowException() {
        // Arrange
        when(allergyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> allergyService.getAllergyById(999L));
        verify(allergyRepository, times(1)).findById(999L);
    }

    @Test
    void createAllergy_ShouldCreateAllergy() {
        // Arrange
        when(modelMapper.map(any(AllergyDTO.class), eq(Allergy.class))).thenReturn(testAllergy);
        when(allergyRepository.save(any(Allergy.class))).thenReturn(testAllergy);
        when(modelMapper.map(any(Allergy.class), eq(AllergyDTO.class))).thenReturn(testAllergyDTO);

        // Act
        AllergyDTO result = allergyService.createAllergy(testAllergyDTO);

        // Assert
        assertNotNull(result);
        verify(allergyRepository, times(1)).save(any(Allergy.class));
    }

    @Test
    void deleteAllergy_WhenAllergyExists_ShouldDeleteAllergy() {
        // Arrange
        when(allergyRepository.existsById(1L)).thenReturn(true);

        // Act
        allergyService.deleteAllergy(1L);

        // Assert
        verify(allergyRepository, times(1)).existsById(1L);
        verify(allergyRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteAllergy_WhenAllergyNotExists_ShouldThrowException() {
        // Arrange
        when(allergyRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> allergyService.deleteAllergy(999L));
        verify(allergyRepository, times(1)).existsById(999L);
        verify(allergyRepository, never()).deleteById(anyLong());
    }
}


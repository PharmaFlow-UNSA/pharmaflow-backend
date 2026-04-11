package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.Therapy;
import com.pharmaflow.userhealth.repositories.TherapyRepository;
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
class TherapyServiceTest {

    @Mock
    private TherapyRepository therapyRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private TherapyService therapyService;

    private Therapy testTherapy;
    private TherapyDTO testTherapyDTO;

    @BeforeEach
    void setUp() {
        testTherapy = new Therapy();
        testTherapy.setId(1L);
        testTherapy.setMedicationName("Aspirin");
        testTherapy.setDosage("500mg");
        testTherapy.setFrequency("2 times daily");

        testTherapyDTO = new TherapyDTO();
        testTherapyDTO.setId(1L);
        testTherapyDTO.setMedicationName("Aspirin");
        testTherapyDTO.setDosage("500mg");
        testTherapyDTO.setFrequency("2 times daily");
    }

    @Test
    void getAllTherapies_ShouldReturnListOfTherapies() {
        when(therapyRepository.findAll()).thenReturn(Arrays.asList(testTherapy));
        when(modelMapper.map(any(Therapy.class), eq(TherapyDTO.class))).thenReturn(testTherapyDTO);

        List<TherapyDTO> result = therapyService.getAllTherapies();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Aspirin", result.get(0).getMedicationName());
        verify(therapyRepository, times(1)).findAll();
    }

    @Test
    void getTherapyById_WhenTherapyExists_ShouldReturnTherapy() {
        when(therapyRepository.findById(1L)).thenReturn(Optional.of(testTherapy));
        when(modelMapper.map(any(Therapy.class), eq(TherapyDTO.class))).thenReturn(testTherapyDTO);

        TherapyDTO result = therapyService.getTherapyById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Aspirin", result.getMedicationName());
        verify(therapyRepository, times(1)).findById(1L);
    }

    @Test
    void getTherapyById_WhenTherapyNotFound_ShouldThrowException() {
        when(therapyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> therapyService.getTherapyById(999L));
        verify(therapyRepository, times(1)).findById(999L);
    }

    @Test
    void createTherapy_ShouldReturnCreatedTherapy() {
        when(modelMapper.map(any(TherapyDTO.class), eq(Therapy.class))).thenReturn(testTherapy);
        when(therapyRepository.save(any(Therapy.class))).thenReturn(testTherapy);
        when(modelMapper.map(any(Therapy.class), eq(TherapyDTO.class))).thenReturn(testTherapyDTO);

        TherapyDTO result = therapyService.createTherapy(testTherapyDTO);

        assertNotNull(result);
        assertEquals("Aspirin", result.getMedicationName());
        verify(therapyRepository, times(1)).save(any(Therapy.class));
    }

    @Test
    void updateTherapy_WhenTherapyExists_ShouldReturnUpdatedTherapy() {
        when(therapyRepository.findById(1L)).thenReturn(Optional.of(testTherapy));
        when(therapyRepository.save(any(Therapy.class))).thenReturn(testTherapy);
        when(modelMapper.map(any(Therapy.class), eq(TherapyDTO.class))).thenReturn(testTherapyDTO);

        TherapyDTO result = therapyService.updateTherapy(1L, testTherapyDTO);

        assertNotNull(result);
        verify(therapyRepository, times(1)).findById(1L);
        verify(therapyRepository, times(1)).save(any(Therapy.class));
    }

    @Test
    void updateTherapy_WhenTherapyNotFound_ShouldThrowException() {
        when(therapyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> therapyService.updateTherapy(999L, testTherapyDTO));
        verify(therapyRepository, times(1)).findById(999L);
        verify(therapyRepository, never()).save(any(Therapy.class));
    }

    @Test
    void deleteTherapy_WhenTherapyExists_ShouldDeleteTherapy() {
        when(therapyRepository.existsById(1L)).thenReturn(true);
        doNothing().when(therapyRepository).deleteById(1L);

        therapyService.deleteTherapy(1L);

        verify(therapyRepository, times(1)).existsById(1L);
        verify(therapyRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTherapy_WhenTherapyNotFound_ShouldThrowException() {
        when(therapyRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> therapyService.deleteTherapy(999L));
        verify(therapyRepository, times(1)).existsById(999L);
        verify(therapyRepository, never()).deleteById(any());
    }
}


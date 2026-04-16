package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.repositories.PatientProfileRepository;
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
class PatientProfileServiceTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PatientProfileService patientProfileService;

    private PatientProfile testProfile;
    private PatientProfileDTO testProfileDTO;

    @BeforeEach
    void setUp() {
        testProfile = new PatientProfile();
        testProfile.setId(1L);
        testProfile.setWeight(75.0);
        testProfile.setHeight(180.0);
        testProfile.setBloodType("A+");

        testProfileDTO = new PatientProfileDTO();
        testProfileDTO.setId(1L);
        testProfileDTO.setWeight(75.0);
        testProfileDTO.setHeight(180.0);
        testProfileDTO.setBloodType("A+");
    }

    @Test
    void getAllPatientProfiles_ShouldReturnListOfProfiles() {
        when(patientProfileRepository.findAll()).thenReturn(Arrays.asList(testProfile));
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class))).thenReturn(testProfileDTO);

        List<PatientProfileDTO> result = patientProfileService.getAllPatientProfiles();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("A+", result.get(0).getBloodType());
        verify(patientProfileRepository, times(1)).findAll();
    }

    @Test
    void getPatientProfileById_WhenProfileExists_ShouldReturnProfile() {
        when(patientProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class))).thenReturn(testProfileDTO);

        PatientProfileDTO result = patientProfileService.getPatientProfileById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(75.0, result.getWeight());
        verify(patientProfileRepository, times(1)).findById(1L);
    }

    @Test
    void getPatientProfileById_WhenProfileNotFound_ShouldThrowException() {
        when(patientProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientProfileService.getPatientProfileById(999L));
        verify(patientProfileRepository, times(1)).findById(999L);
    }

    @Test
    void createPatientProfile_ShouldReturnCreatedProfile() {
        when(modelMapper.map(any(PatientProfileDTO.class), eq(PatientProfile.class))).thenReturn(testProfile);
        when(patientProfileRepository.save(any(PatientProfile.class))).thenReturn(testProfile);
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class))).thenReturn(testProfileDTO);

        PatientProfileDTO result = patientProfileService.createPatientProfile(testProfileDTO);

        assertNotNull(result);
        assertEquals("A+", result.getBloodType());
        verify(patientProfileRepository, times(1)).save(any(PatientProfile.class));
    }

    @Test
    void updatePatientProfile_WhenProfileExists_ShouldReturnUpdatedProfile() {
        when(patientProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(patientProfileRepository.save(any(PatientProfile.class))).thenReturn(testProfile);
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class))).thenReturn(testProfileDTO);

        // ModelMapper.map(DTO, entity) doesn't return anything - it modifies in place
        doNothing().when(modelMapper).map(any(PatientProfileDTO.class), any(PatientProfile.class));

        PatientProfileDTO result = patientProfileService.updatePatientProfile(1L, testProfileDTO);

        assertNotNull(result);
        verify(patientProfileRepository, times(1)).findById(1L);
        verify(patientProfileRepository, times(1)).save(any(PatientProfile.class));
    }

    @Test
    void updatePatientProfile_WhenProfileNotFound_ShouldThrowException() {
        when(patientProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> patientProfileService.updatePatientProfile(999L, testProfileDTO));
        verify(patientProfileRepository, times(1)).findById(999L);
        verify(patientProfileRepository, never()).save(any(PatientProfile.class));
    }

    @Test
    void deletePatientProfile_WhenProfileExists_ShouldDeleteProfile() {
        when(patientProfileRepository.existsById(1L)).thenReturn(true);
        doNothing().when(patientProfileRepository).deleteById(1L);

        patientProfileService.deletePatientProfile(1L);

        verify(patientProfileRepository, times(1)).existsById(1L);
        verify(patientProfileRepository, times(1)).deleteById(1L);
    }

    @Test
    void deletePatientProfile_WhenProfileNotFound_ShouldThrowException() {
        when(patientProfileRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> patientProfileService.deletePatientProfile(999L));
        verify(patientProfileRepository, times(1)).existsById(999L);
        verify(patientProfileRepository, never()).deleteById(any());
    }
}


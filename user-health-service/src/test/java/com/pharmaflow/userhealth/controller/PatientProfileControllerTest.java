package com.pharmaflow.userhealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.service.PatientProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientProfileController.class)
class PatientProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientProfileService patientProfileService;

    private PatientProfileDTO testProfileDTO;

    @BeforeEach
    void setUp() {
        testProfileDTO = new PatientProfileDTO();
        testProfileDTO.setId(1L);
        testProfileDTO.setWeight(75.0);
        testProfileDTO.setHeight(180.0);
        testProfileDTO.setBloodType("A+");
        testProfileDTO.setAllergies(new ArrayList<>());
        testProfileDTO.setTherapies(new ArrayList<>());
    }

    @Test
    void getAllPatientProfiles_ShouldReturn200AndListOfProfiles() throws Exception {
        List<PatientProfileDTO> profiles = Arrays.asList(testProfileDTO);
        when(patientProfileService.getAllPatientProfiles()).thenReturn(profiles);

        mockMvc.perform(get("/api/patient-profiles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].weight").value(75.0))
                .andExpect(jsonPath("$[0].bloodType").value("A+"));

        verify(patientProfileService, times(1)).getAllPatientProfiles();
    }

    @Test
    void getPatientProfileById_WhenProfileExists_ShouldReturn200AndProfile() throws Exception {
        when(patientProfileService.getPatientProfileById(1L)).thenReturn(testProfileDTO);

        mockMvc.perform(get("/api/patient-profiles/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weight").value(75.0));

        verify(patientProfileService, times(1)).getPatientProfileById(1L);
    }

    @Test
    void getPatientProfileById_WhenProfileNotFound_ShouldReturn404() throws Exception {
        when(patientProfileService.getPatientProfileById(999L))
                .thenThrow(new ResourceNotFoundException("Patient profile not found with id: 999"));

        mockMvc.perform(get("/api/patient-profiles/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(patientProfileService, times(1)).getPatientProfileById(999L);
    }

    @Test
    void createPatientProfile_WithValidData_ShouldReturn201AndCreatedProfile() throws Exception {
        when(patientProfileService.createPatientProfile(any(PatientProfileDTO.class))).thenReturn(testProfileDTO);

        mockMvc.perform(post("/api/patient-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProfileDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weight").value(75.0));

        verify(patientProfileService, times(1)).createPatientProfile(any(PatientProfileDTO.class));
    }

    @Test
    void createPatientProfile_WithInvalidData_ShouldReturn400() throws Exception {
        PatientProfileDTO invalidDTO = new PatientProfileDTO();
        invalidDTO.setWeight(600.0); // Too heavy
        invalidDTO.setHeight(10.0); // Too short
        invalidDTO.setBloodType("XYZ"); // Invalid blood type

        mockMvc.perform(post("/api/patient-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(patientProfileService, never()).createPatientProfile(any(PatientProfileDTO.class));
    }

    @Test
    void updatePatientProfile_WithValidData_ShouldReturn200AndUpdatedProfile() throws Exception {
        when(patientProfileService.updatePatientProfile(eq(1L), any(PatientProfileDTO.class)))
                .thenReturn(testProfileDTO);

        mockMvc.perform(put("/api/patient-profiles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProfileDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weight").value(75.0));

        verify(patientProfileService, times(1)).updatePatientProfile(eq(1L), any(PatientProfileDTO.class));
    }

    @Test
    void deletePatientProfile_WhenProfileExists_ShouldReturn204() throws Exception {
        doNothing().when(patientProfileService).deletePatientProfile(1L);

        mockMvc.perform(delete("/api/patient-profiles/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(patientProfileService, times(1)).deletePatientProfile(1L);
    }

    @Test
    void deletePatientProfile_WhenProfileNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Patient profile not found with id: 999"))
                .when(patientProfileService).deletePatientProfile(999L);

        mockMvc.perform(delete("/api/patient-profiles/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(patientProfileService, times(1)).deletePatientProfile(999L);
    }
}


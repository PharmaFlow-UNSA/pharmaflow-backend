package com.pharmaflow.userhealth.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.models.enums.BloodType;
import com.pharmaflow.userhealth.service.PatientProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import java.util.ArrayList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PatientProfileController.class)
class PatientProfileControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PatientProfileService patientProfileService;
    private PatientProfileDTO testProfileDTO;
    @BeforeEach
    void setUp() {
        testProfileDTO = new PatientProfileDTO();
        testProfileDTO.setId(1L);
        testProfileDTO.setWeight(75.0);
        testProfileDTO.setBloodType(BloodType.A_POSITIVE);
        testProfileDTO.setAllergies(new ArrayList<>());
        testProfileDTO.setTherapies(new ArrayList<>());
    }
    @Test
    void getAllProfiles() throws Exception {
        Page<PatientProfileDTO> page = new PageImpl<>(Arrays.asList(testProfileDTO));
        when(patientProfileService.findAll(any(), any(), any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/patient-profiles")).andExpect(status().isOk());
        verify(patientProfileService, times(1)).findAll(any(), any(), any(), any());
    }
    @Test
    void getProfileById() throws Exception {
        when(patientProfileService.getPatientProfileById(1L)).thenReturn(testProfileDTO);
        mockMvc.perform(get("/api/patient-profiles/1")).andExpect(status().isOk());
        verify(patientProfileService, times(1)).getPatientProfileById(1L);
    }
    @Test
    void createProfile() throws Exception {
        when(patientProfileService.createPatientProfile(any())).thenReturn(testProfileDTO);
        mockMvc.perform(post("/api/patient-profiles").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(testProfileDTO))).andExpect(status().isCreated());
        verify(patientProfileService, times(1)).createPatientProfile(any());
    }
}
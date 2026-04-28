package com.pharmaflow.userhealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.service.TherapyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TherapyController.class)
class TherapyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TherapyService therapyService;

    private TherapyDTO testTherapyDTO;

    @BeforeEach
    void setUp() {
        testTherapyDTO = new TherapyDTO();
        testTherapyDTO.setId(1L);
        testTherapyDTO.setMedicationName("Aspirin");
        testTherapyDTO.setDosage("500mg");
        testTherapyDTO.setFrequency("2 times daily");
    }

    @Test
    void getAllTherapies_ShouldReturn200AndListOfTherapies() throws Exception {
        List<TherapyDTO> therapies = Arrays.asList(testTherapyDTO);
        when(therapyService.getAllTherapies()).thenReturn(therapies);

        mockMvc.perform(get("/api/therapies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].medicationName").value("Aspirin"))
                .andExpect(jsonPath("$[0].dosage").value("500mg"));

        verify(therapyService, times(1)).getAllTherapies();
    }

    @Test
    void getTherapies_WithMedicationNameFilter_ShouldReturn200AndFilteredTherapies() throws Exception {
        List<TherapyDTO> therapies = Arrays.asList(testTherapyDTO);
        when(therapyService.findByMedicationNameContaining("Aspirin")).thenReturn(therapies);

        mockMvc.perform(get("/api/therapies")
                        .param("medicationName", "Aspirin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].medicationName").value("Aspirin"));

        verify(therapyService, times(1)).findByMedicationNameContaining("Aspirin");
        verify(therapyService, never()).getAllTherapies();
    }

    @Test
    void getTherapyById_WhenTherapyExists_ShouldReturn200AndTherapy() throws Exception {
        when(therapyService.getTherapyById(1L)).thenReturn(testTherapyDTO);

        mockMvc.perform(get("/api/therapies/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.medicationName").value("Aspirin"));

        verify(therapyService, times(1)).getTherapyById(1L);
    }

    @Test
    void getTherapyById_WhenTherapyNotFound_ShouldReturn404() throws Exception {
        when(therapyService.getTherapyById(999L))
                .thenThrow(new ResourceNotFoundException("Therapy not found with id: 999"));

        mockMvc.perform(get("/api/therapies/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(therapyService, times(1)).getTherapyById(999L);
    }

    @Test
    void createTherapy_WithValidData_ShouldReturn201AndCreatedTherapy() throws Exception {
        when(therapyService.createTherapy(any(TherapyDTO.class))).thenReturn(testTherapyDTO);

        mockMvc.perform(post("/api/therapies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTherapyDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.medicationName").value("Aspirin"));

        verify(therapyService, times(1)).createTherapy(any(TherapyDTO.class));
    }

    @Test
    void createTherapies_WithBulkParameter_ShouldReturn201AndCreatedTherapies() throws Exception {
        TherapyDTO therapy2 = new TherapyDTO();
        therapy2.setId(2L);
        therapy2.setMedicationName("Ibuprofen");
        therapy2.setDosage("400mg");
        therapy2.setFrequency("3 times daily");

        List<TherapyDTO> therapies = Arrays.asList(testTherapyDTO, therapy2);
        when(therapyService.createTherapiesBatch(anyList())).thenReturn(therapies);

        mockMvc.perform(post("/api/therapies")
                        .param("bulk", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(therapies)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].medicationName").value("Aspirin"))
                .andExpect(jsonPath("$[1].medicationName").value("Ibuprofen"));

        verify(therapyService, times(1)).createTherapiesBatch(anyList());
    }

    @Test
    void createTherapy_WithInvalidData_ShouldReturn400() throws Exception {
        TherapyDTO invalidDTO = new TherapyDTO();
        invalidDTO.setMedicationName("A"); // Too short
        invalidDTO.setDosage("invalid@@@");
        invalidDTO.setFrequency("???");

        mockMvc.perform(post("/api/therapies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(therapyService, never()).createTherapy(any(TherapyDTO.class));
        verify(therapyService, never()).createTherapiesBatch(anyList());
    }

    @Test
    void updateTherapy_WithValidData_ShouldReturn200AndUpdatedTherapy() throws Exception {
        when(therapyService.updateTherapy(eq(1L), any(TherapyDTO.class))).thenReturn(testTherapyDTO);

        mockMvc.perform(put("/api/therapies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTherapyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.medicationName").value("Aspirin"));

        verify(therapyService, times(1)).updateTherapy(eq(1L), any(TherapyDTO.class));
    }

    @Test
    void deleteTherapy_WhenTherapyExists_ShouldReturn204() throws Exception {
        doNothing().when(therapyService).deleteTherapy(1L);

        mockMvc.perform(delete("/api/therapies/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(therapyService, times(1)).deleteTherapy(1L);
    }

    @Test
    void deleteTherapy_WhenTherapyNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Therapy not found with id: 999"))
                .when(therapyService).deleteTherapy(999L);

        mockMvc.perform(delete("/api/therapies/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(therapyService, times(1)).deleteTherapy(999L);
    }
}


package com.pharmaflow.userhealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.service.AllergyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

@WebMvcTest(AllergyController.class)
@AutoConfigureMockMvc(addFilters = false)
class AllergyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AllergyService allergyService;

    private AllergyDTO testAllergyDTO;

    @BeforeEach
    void setUp() {
        testAllergyDTO = new AllergyDTO();
        testAllergyDTO.setId(1L);
        testAllergyDTO.setAllergen("Penicillin");
        testAllergyDTO.setSeverity("High");
        testAllergyDTO.setActiveSubstance("Penicillin G");
    }

    @Test
    void getAllAllergies_ShouldReturn200AndListOfAllergies() throws Exception {
        // Arrange
        List<AllergyDTO> allergies = Arrays.asList(testAllergyDTO);
        when(allergyService.getAllAllergies()).thenReturn(allergies);

        // Act & Assert
        mockMvc.perform(get("/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].allergen").value("Penicillin"))
                .andExpect(jsonPath("$[0].severity").value("High"));

        verify(allergyService, times(1)).getAllAllergies();
    }

    @Test
    void getAllergyById_WhenAllergyExists_ShouldReturn200AndAllergy() throws Exception {
        // Arrange
        when(allergyService.getAllergyById(1L)).thenReturn(testAllergyDTO);

        // Act & Assert
        mockMvc.perform(get("/api/allergies/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.allergen").value("Penicillin"));

        verify(allergyService, times(1)).getAllergyById(1L);
    }

    @Test
    void getAllergyById_WhenAllergyNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(allergyService.getAllergyById(999L))
                .thenThrow(new ResourceNotFoundException("Allergy not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/allergies/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(allergyService, times(1)).getAllergyById(999L);
    }

    @Test
    void createAllergy_WithValidData_ShouldReturn201AndCreatedAllergy() throws Exception {
        // Arrange
        when(allergyService.createAllergy(any(AllergyDTO.class))).thenReturn(testAllergyDTO);

        // Act & Assert
        mockMvc.perform(post("/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAllergyDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.allergen").value("Penicillin"));

        verify(allergyService, times(1)).createAllergy(any(AllergyDTO.class));
    }

    @Test
    void createAllergy_WithInvalidData_ShouldReturn400() throws Exception {
        // Arrange - Empty allergen
        AllergyDTO invalidDTO = new AllergyDTO();
        invalidDTO.setAllergen(""); // Invalid - blank
        invalidDTO.setSeverity("High");

        // Act & Assert
        mockMvc.perform(post("/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(allergyService, never()).createAllergy(any(AllergyDTO.class));
    }

    @Test
    void updateAllergy_WithValidData_ShouldReturn200AndUpdatedAllergy() throws Exception {
        // Arrange
        when(allergyService.updateAllergy(eq(1L), any(AllergyDTO.class))).thenReturn(testAllergyDTO);

        // Act & Assert
        mockMvc.perform(put("/api/allergies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAllergyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.allergen").value("Penicillin"));

        verify(allergyService, times(1)).updateAllergy(eq(1L), any(AllergyDTO.class));
    }

    @Test
    void deleteAllergy_WhenAllergyExists_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(allergyService).deleteAllergy(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/allergies/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(allergyService, times(1)).deleteAllergy(1L);
    }
}


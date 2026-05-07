package com.pharmaflow.producthealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.producthealth.controllers.DrugInteractionController;
import com.pharmaflow.producthealth.dto.DrugInteractionDTO;
import com.pharmaflow.producthealth.exception.GlobalExceptionHandler;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.services.DrugInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DrugInteractionController.class)
@Import(GlobalExceptionHandler.class)
class DrugInteractionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DrugInteractionService drugInteractionService;

    private DrugInteractionDTO testDTO;
    private DrugInteractionDTO validCreateDTO;

    @BeforeEach
    void setUp() {
        testDTO = new DrugInteractionDTO(1L, 1L, 2L, "Ibuprofen", "Varfarin",
                "MAJOR", "Increased bleeding risk when combining ibuprofen and warfarin", "Avoid combination");

        validCreateDTO = new DrugInteractionDTO();
        validCreateDTO.setSubstanceAId(1L);
        validCreateDTO.setSubstanceBId(2L);
        validCreateDTO.setSeverity("MAJOR");
        validCreateDTO.setDescription("Description of drug interaction between two substances over ten characters");
    }

    @Test
    void getAllInteractions_shouldReturn200AndList() throws Exception {
        when(drugInteractionService.getAllInteractions()).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/interactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].severity").value("MAJOR"))
                .andExpect(jsonPath("$[0].substanceAName").value("Ibuprofen"));
    }

    @Test
    void getInteractionById_whenExists_shouldReturn200() throws Exception {
        when(drugInteractionService.getInteractionById(1L)).thenReturn(testDTO);

        mockMvc.perform(get("/api/interactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.severity").value("MAJOR"));
    }

    @Test
    void getInteractionById_whenNotExists_shouldReturn404() throws Exception {
        when(drugInteractionService.getInteractionById(999L))
                .thenThrow(new ResourceNotFoundException("Drug interaction with ID 999 not found."));

        mockMvc.perform(get("/api/interactions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createInteraction_withValidData_shouldReturn201() throws Exception {
        when(drugInteractionService.createInteraction(any(DrugInteractionDTO.class))).thenReturn(testDTO);

        mockMvc.perform(post("/api/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.severity").value("MAJOR"));

        verify(drugInteractionService, times(1)).createInteraction(any());
    }

    @Test
    void createInteraction_withInvalidSeverity_shouldReturn400() throws Exception {
        validCreateDTO.setSeverity("INVALID");

        mockMvc.perform(post("/api/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.errors.severity").exists());

        verify(drugInteractionService, never()).createInteraction(any());
    }

    @Test
    void createInteraction_withMissingDescription_shouldReturn400() throws Exception {
        validCreateDTO.setDescription(null);

        mockMvc.perform(post("/api/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void deleteInteraction_whenNotExists_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Drug interaction with ID 999 not found."))
                .when(drugInteractionService).deleteInteraction(999L);

        mockMvc.perform(delete("/api/interactions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }
}

package com.pharmaflow.userhealth.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.models.enums.Severity;
import com.pharmaflow.userhealth.service.AllergyService;
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
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(AllergyController.class)
class AllergyControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AllergyService allergyService;
    private AllergyDTO testAllergyDTO;
    @BeforeEach
    void setUp() {
        testAllergyDTO = new AllergyDTO();
        testAllergyDTO.setId(1L);
        testAllergyDTO.setAllergen("Penicillin");
        testAllergyDTO.setSeverity(Severity.HIGH);
    }
    @Test
    void getAllAllergies() throws Exception {
        Page<AllergyDTO> page = new PageImpl<>(Arrays.asList(testAllergyDTO));
        when(allergyService.findAll(any(), any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/allergies")).andExpect(status().isOk());
        verify(allergyService, times(1)).findAll(any(), any(), any());
    }
    @Test
    void getAllergyById() throws Exception {
        when(allergyService.getAllergyById(1L)).thenReturn(testAllergyDTO);
        mockMvc.perform(get("/api/allergies/1")).andExpect(status().isOk());
        verify(allergyService, times(1)).getAllergyById(1L);
    }
    @Test
    void createAllergy() throws Exception {
        when(allergyService.createAllergy(any())).thenReturn(testAllergyDTO);
        mockMvc.perform(post("/api/allergies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testAllergyDTO)))
            .andExpect(status().isCreated());
        verify(allergyService, times(1)).createAllergy(any());
    }
    @Test
    void deleteAllergy() throws Exception {
        doNothing().when(allergyService).deleteAllergy(1L);
        mockMvc.perform(delete("/api/allergies/1")).andExpect(status().isNoContent());
        verify(allergyService, times(1)).deleteAllergy(1L);
    }
}
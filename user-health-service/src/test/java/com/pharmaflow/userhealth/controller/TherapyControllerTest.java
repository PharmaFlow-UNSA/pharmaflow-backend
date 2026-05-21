package com.pharmaflow.userhealth.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.service.TherapyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(TherapyController.class)
class TherapyControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TherapyService therapyService;
    private TherapyDTO testTherapyDTO;
    @BeforeEach
    void setUp() {
        testTherapyDTO = new TherapyDTO();
        testTherapyDTO.setId(1L);
        testTherapyDTO.setMedicationName("Aspirin");
    }
    @Test
    @WithMockUser(roles = "USER")
    void getAllTherapies() throws Exception {
        Page<TherapyDTO> page = new PageImpl<>(Arrays.asList(testTherapyDTO));
        when(therapyService.findAll(any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/therapies")).andExpect(status().isOk());
        verify(therapyService, times(1)).findAll(any(), any());
    }
    @Test
    @WithMockUser(roles = "USER")
    void getTherapyById() throws Exception {
        when(therapyService.getTherapyById(1L)).thenReturn(testTherapyDTO);
        mockMvc.perform(get("/api/therapies/1")).andExpect(status().isOk());
        verify(therapyService, times(1)).getTherapyById(1L);
    }
    @Test
    @WithMockUser(roles = "DOCTOR")
    void createTherapy() throws Exception {
        when(therapyService.createTherapy(any())).thenReturn(testTherapyDTO);
        mockMvc.perform(post("/api/therapies")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTherapyDTO)))
                .andExpect(status().isCreated());
        verify(therapyService, times(1)).createTherapy(any());
    }
}
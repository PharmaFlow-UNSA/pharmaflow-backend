package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrescriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private PrescriptionService prescriptionService;

    private PrescriptionDTO testDTO;
    private PrescriptionCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        testDTO = new PrescriptionDTO();
        testDTO.setId(1L);
        testDTO.setUserId(10L);
        testDTO.setImageUrl("https://example.com/recept.png");
        testDTO.setStatus("PENDING");
        testDTO.setUploadedAt(LocalDateTime.now());

        testCreateDTO = new PrescriptionCreateDTO();
        testCreateDTO.setUserId(10L);
        testCreateDTO.setImageUrl("https://example.com/recept.png");
    }

    @Test
    void getPrescriptions_ShouldReturn200AndPagedResult() throws Exception {
        Page<PrescriptionDTO> page = new PageImpl<>(List.of(testDTO));
        when(prescriptionService.findAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/prescriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getPrescriptionById_WhenExists_ShouldReturn200() throws Exception {
        when(prescriptionService.getPrescriptionById(1L)).thenReturn(testDTO);

        mockMvc.perform(get("/api/prescriptions/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPrescriptionById_WhenNotExists_ShouldReturn404() throws Exception {
        when(prescriptionService.getPrescriptionById(999L))
                .thenThrow(new ResourceNotFoundException("Prescription not found with id: 999"));

        mockMvc.perform(get("/api/prescriptions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPrescriptionsByUserId_ShouldReturn200() throws Exception {
        when(prescriptionService.getPrescriptionsByUserId(10L)).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/prescriptions/user/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getPrescriptionsByStatus_ShouldReturn200() throws Exception {
        when(prescriptionService.getPrescriptionsByStatus("PENDING")).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/prescriptions/status/PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void createPrescription_WithValidData_ShouldReturn201() throws Exception {
        when(prescriptionService.createPrescription(any(PrescriptionCreateDTO.class))).thenReturn(testDTO);

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void createPrescription_WithInvalidData_ShouldReturn400() throws Exception {
        PrescriptionCreateDTO invalid = new PrescriptionCreateDTO();

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPrescriptionsBatch_ShouldReturn201() throws Exception {
        when(prescriptionService.createPrescriptionsBatch(anyList())).thenReturn(List.of(testDTO));

        mockMvc.perform(post("/api/prescriptions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testCreateDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    void updatePrescription_WithValidData_ShouldReturn200() throws Exception {
        when(prescriptionService.updatePrescription(eq(1L), any(PrescriptionDTO.class))).thenReturn(testDTO);

        mockMvc.perform(put("/api/prescriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchPrescription_WithValidPatch_ShouldReturn200() throws Exception {
        when(prescriptionService.patchPrescription(eq(1L), anyString())).thenReturn(testDTO);

        mockMvc.perform(patch("/api/prescriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"APPROVED\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchPrescription_WithInvalidPatch_ShouldReturn400() throws Exception {
        when(prescriptionService.patchPrescription(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/prescriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletePrescription_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(prescriptionService).deletePrescription(1L);

        mockMvc.perform(delete("/api/prescriptions/1"))
                .andExpect(status().isNoContent());
    }
}

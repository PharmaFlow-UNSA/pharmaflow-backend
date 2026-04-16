package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private PrescriptionService prescriptionService;

    private PrescriptionDTO prescriptionDTO;
    private PrescriptionCreateDTO prescriptionCreateDTO;

    @BeforeEach
    void setUp() {
        prescriptionDTO = new PrescriptionDTO();
        prescriptionDTO.setId(1L);
        prescriptionDTO.setUserId(1L);
        prescriptionDTO.setImageUrl("/uploads/recept_1.pdf");
        prescriptionDTO.setStatus("PENDING");
        prescriptionDTO.setUploadedAt(LocalDateTime.of(2026, 4, 16, 10, 0));
        prescriptionDTO.setReviewedAt(null);
        prescriptionDTO.setReviewerNotes(null);
        prescriptionDTO.setOrderIds(Collections.emptyList());
        prescriptionDTO.setAutoRefillSubscriptionIds(Collections.emptyList());

        prescriptionCreateDTO = new PrescriptionCreateDTO();
        prescriptionCreateDTO.setUserId(1L);
        prescriptionCreateDTO.setImageUrl("/uploads/recept_1.pdf");
    }

    @Test
    void getAllPrescriptions_ShouldReturn200AndList() throws Exception {
        List<PrescriptionDTO> prescriptions = Arrays.asList(prescriptionDTO);
        when(prescriptionService.getAllPrescriptions()).thenReturn(prescriptions);

        mockMvc.perform(get("/api/prescriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].imageUrl").value("/uploads/recept_1.pdf"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(prescriptionService, times(1)).getAllPrescriptions();
    }

    @Test
    void getPrescriptionById_WhenExists_ShouldReturn200() throws Exception {
        when(prescriptionService.getPrescriptionById(1L)).thenReturn(prescriptionDTO);

        mockMvc.perform(get("/api/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.imageUrl").value("/uploads/recept_1.pdf"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(prescriptionService, times(1)).getPrescriptionById(1L);
    }

    @Test
    void getPrescriptionById_WhenNotExists_ShouldReturn404() throws Exception {
        when(prescriptionService.getPrescriptionById(999L))
                .thenThrow(new ResourceNotFoundException("Prescription not found with id: 999"));

        mockMvc.perform(get("/api/prescriptions/999"))
                .andExpect(status().isNotFound());

        verify(prescriptionService, times(1)).getPrescriptionById(999L);
    }

    @Test
    void getPrescriptionsByUserId_ShouldReturn200AndList() throws Exception {
        List<PrescriptionDTO> prescriptions = Arrays.asList(prescriptionDTO);
        when(prescriptionService.getPrescriptionsByUserId(1L)).thenReturn(prescriptions);

        mockMvc.perform(get("/api/prescriptions/user/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(prescriptionService, times(1)).getPrescriptionsByUserId(1L);
    }

    @Test
    void createPrescription_WithValidData_ShouldReturn201() throws Exception {
        when(prescriptionService.createPrescription(any(PrescriptionCreateDTO.class)))
                .thenReturn(prescriptionDTO);

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prescriptionCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.imageUrl").value("/uploads/recept_1.pdf"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(prescriptionService, times(1)).createPrescription(any(PrescriptionCreateDTO.class));
    }

    @Test
    void createPrescription_WithInvalidData_ShouldReturn400() throws Exception {
        PrescriptionCreateDTO invalidDTO = new PrescriptionCreateDTO();
        invalidDTO.setUserId(1L);
        invalidDTO.setImageUrl("");

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(prescriptionService, never()).createPrescription(any(PrescriptionCreateDTO.class));
    }

    @Test
    void updatePrescription_WithValidData_ShouldReturn200() throws Exception {
        when(prescriptionService.updatePrescription(eq(1L), any(PrescriptionDTO.class)))
                .thenReturn(prescriptionDTO);

        mockMvc.perform(put("/api/prescriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prescriptionDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(prescriptionService, times(1)).updatePrescription(eq(1L), any(PrescriptionDTO.class));
    }

    @Test
    void deletePrescription_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(prescriptionService).deletePrescription(1L);

        mockMvc.perform(delete("/api/prescriptions/1"))
                .andExpect(status().isNoContent());

        verify(prescriptionService, times(1)).deletePrescription(1L);
    }
}

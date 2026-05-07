package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.ReservationService;
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

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private ReservationService reservationService;

    private ReservationDTO testReservationDTO;

    @BeforeEach
    void setUp() {
        testReservationDTO = new ReservationDTO();
        testReservationDTO.setId(1L);
        testReservationDTO.setUserId(10L);
        testReservationDTO.setProductId(100L);
        testReservationDTO.setQuantity(2);
        testReservationDTO.setStatus("PENDING");
        testReservationDTO.setReservedAt(LocalDateTime.of(2026, 5, 5, 10, 0));
        testReservationDTO.setExpiresAt(LocalDateTime.of(2026, 5, 6, 10, 0));
        testReservationDTO.setPharmacyId(1L);
    }

    @Test
    void getReservations_ShouldReturn200AndPagedResult() throws Exception {
        Page<ReservationDTO> page = new PageImpl<>(List.of(testReservationDTO));
        when(reservationService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void getReservationById_WhenExists_ShouldReturn200() throws Exception {
        when(reservationService.getReservationById(1L)).thenReturn(testReservationDTO);

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getReservationById_WhenNotExists_ShouldReturn404() throws Exception {
        when(reservationService.getReservationById(999L))
                .thenThrow(new ResourceNotFoundException("Reservation not found with id: 999"));

        mockMvc.perform(get("/api/reservations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReservationsByPharmacyId_ShouldReturn200() throws Exception {
        when(reservationService.getReservationsByPharmacyId(1L)).thenReturn(List.of(testReservationDTO));

        mockMvc.perform(get("/api/reservations/pharmacy/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getReservationsByUserId_ShouldReturn200() throws Exception {
        when(reservationService.getReservationsByUserId(10L)).thenReturn(List.of(testReservationDTO));

        mockMvc.perform(get("/api/reservations/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getReservationsByStatus_ShouldReturn200() throws Exception {
        when(reservationService.getReservationsByStatus("PENDING")).thenReturn(List.of(testReservationDTO));

        mockMvc.perform(get("/api/reservations/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createReservation_WithValidData_ShouldReturn201() throws Exception {
        when(reservationService.createReservation(any(ReservationDTO.class))).thenReturn(testReservationDTO);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReservationDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void createReservation_WithInvalidStatus_ShouldReturn400() throws Exception {
        ReservationDTO invalid = new ReservationDTO();
        invalid.setUserId(1L);
        invalid.setProductId(1L);
        invalid.setQuantity(1);
        invalid.setStatus("BOGUS");
        invalid.setReservedAt(LocalDateTime.now());
        invalid.setPharmacyId(1L);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservationsBatch_ShouldReturn201() throws Exception {
        when(reservationService.createReservationsBatch(anyList())).thenReturn(List.of(testReservationDTO));

        mockMvc.perform(post("/api/reservations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testReservationDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateReservation_WithValidData_ShouldReturn200() throws Exception {
        when(reservationService.updateReservation(eq(1L), any(ReservationDTO.class))).thenReturn(testReservationDTO);

        mockMvc.perform(put("/api/reservations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReservationDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchReservation_WithValidPatch_ShouldReturn200() throws Exception {
        when(reservationService.patchReservation(eq(1L), anyString())).thenReturn(testReservationDTO);

        mockMvc.perform(patch("/api/reservations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"READY\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchReservation_WhenInvalidPatch_ShouldReturn400() throws Exception {
        when(reservationService.patchReservation(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/reservations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReservation_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(reservationService).deleteReservation(1L);

        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isNoContent());
    }
}

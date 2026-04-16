package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private ReservationService reservationService;

    private ReservationDTO testReservationDTO;

    @BeforeEach
    void setUp() {
        testReservationDTO = new ReservationDTO();
        testReservationDTO.setId(1L);
        testReservationDTO.setUserId(1L);
        testReservationDTO.setProductId(100L);
        testReservationDTO.setQuantity(1);
        testReservationDTO.setStatus("PENDING");
        testReservationDTO.setReservedAt(LocalDateTime.of(2026, 4, 16, 10, 0));
        testReservationDTO.setExpiresAt(LocalDateTime.of(2026, 4, 17, 10, 0));
        testReservationDTO.setPharmacyId(1L);
    }

    @Test
    void getAllReservations_ShouldReturn200AndList() throws Exception {
        when(reservationService.getAllReservations()).thenReturn(List.of(testReservationDTO));

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(reservationService, times(1)).getAllReservations();
    }

    @Test
    void getReservationById_WhenExists_ShouldReturn200() throws Exception {
        when(reservationService.getReservationById(1L)).thenReturn(testReservationDTO);

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(reservationService, times(1)).getReservationById(1L);
    }

    @Test
    void getReservationById_WhenNotExists_ShouldReturn404() throws Exception {
        when(reservationService.getReservationById(999L))
                .thenThrow(new ResourceNotFoundException("Reservation not found with id: 999"));

        mockMvc.perform(get("/api/reservations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(reservationService, times(1)).getReservationById(999L);
    }

    @Test
    void getReservationsByUserId_ShouldReturn200AndList() throws Exception {
        when(reservationService.getReservationsByUserId(1L)).thenReturn(List.of(testReservationDTO));

        mockMvc.perform(get("/api/reservations/user/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(reservationService, times(1)).getReservationsByUserId(1L);
    }

    @Test
    void createReservation_WithValidData_ShouldReturn201() throws Exception {
        when(reservationService.createReservation(any(ReservationDTO.class))).thenReturn(testReservationDTO);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReservationDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(reservationService, times(1)).createReservation(any(ReservationDTO.class));
    }

    @Test
    void createReservation_WithInvalidData_ShouldReturn400() throws Exception {
        ReservationDTO invalidDTO = new ReservationDTO();
        invalidDTO.setUserId(1L);
        invalidDTO.setProductId(100L);
        invalidDTO.setQuantity(1);
        invalidDTO.setStatus("");
        invalidDTO.setReservedAt(LocalDateTime.of(2026, 4, 16, 10, 0));
        invalidDTO.setPharmacyId(1L);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(reservationService, never()).createReservation(any(ReservationDTO.class));
    }

    @Test
    void updateReservation_WithValidData_ShouldReturn200() throws Exception {
        ReservationDTO updatedDTO = new ReservationDTO();
        updatedDTO.setId(1L);
        updatedDTO.setUserId(1L);
        updatedDTO.setProductId(100L);
        updatedDTO.setQuantity(2);
        updatedDTO.setStatus("READY");
        updatedDTO.setReservedAt(LocalDateTime.of(2026, 4, 16, 10, 0));
        updatedDTO.setExpiresAt(LocalDateTime.of(2026, 4, 17, 10, 0));
        updatedDTO.setPharmacyId(1L);

        when(reservationService.updateReservation(eq(1L), any(ReservationDTO.class))).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/reservations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("READY"));

        verify(reservationService, times(1)).updateReservation(eq(1L), any(ReservationDTO.class));
    }

    @Test
    void deleteReservation_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(reservationService).deleteReservation(1L);

        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isNoContent());

        verify(reservationService, times(1)).deleteReservation(1L);
    }
}

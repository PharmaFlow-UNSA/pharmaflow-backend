package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.pharmacyinventory.dto.DeliveryDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.DeliveryService;
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

@WebMvcTest(DeliveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private DeliveryService deliveryService;

    private DeliveryDTO testDeliveryDTO;

    @BeforeEach
    void setUp() {
        testDeliveryDTO = new DeliveryDTO();
        testDeliveryDTO.setId(1L);
        testDeliveryDTO.setOrderId(100L);
        testDeliveryDTO.setDeliveryAddress("Marsala Tita 10, Sarajevo");
        testDeliveryDTO.setStatus("PREPARING");
        testDeliveryDTO.setEstimatedDelivery(LocalDateTime.of(2026, 5, 6, 12, 0));
        testDeliveryDTO.setActualDelivery(null);
        testDeliveryDTO.setPharmacyId(1L);
    }

    @Test
    void getDeliveries_ShouldReturn200AndPagedResult() throws Exception {
        Page<DeliveryDTO> page = new PageImpl<>(List.of(testDeliveryDTO));
        when(deliveryService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PREPARING"));
    }

    @Test
    void getDeliveryById_WhenExists_ShouldReturn200() throws Exception {
        when(deliveryService.getDeliveryById(1L)).thenReturn(testDeliveryDTO);

        mockMvc.perform(get("/api/deliveries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getDeliveryById_WhenNotExists_ShouldReturn404() throws Exception {
        when(deliveryService.getDeliveryById(999L))
                .thenThrow(new ResourceNotFoundException("Delivery not found with id: 999"));

        mockMvc.perform(get("/api/deliveries/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDeliveriesByPharmacyId_ShouldReturn200() throws Exception {
        when(deliveryService.getDeliveriesByPharmacyId(1L)).thenReturn(List.of(testDeliveryDTO));

        mockMvc.perform(get("/api/deliveries/pharmacy/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getDeliveriesByOrderId_ShouldReturn200() throws Exception {
        when(deliveryService.getDeliveriesByOrderId(100L)).thenReturn(List.of(testDeliveryDTO));

        mockMvc.perform(get("/api/deliveries/order/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getDeliveriesByStatus_ShouldReturn200() throws Exception {
        when(deliveryService.getDeliveriesByStatus("PREPARING")).thenReturn(List.of(testDeliveryDTO));

        mockMvc.perform(get("/api/deliveries/status/PREPARING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createDelivery_WithValidData_ShouldReturn201() throws Exception {
        when(deliveryService.createDelivery(any(DeliveryDTO.class))).thenReturn(testDeliveryDTO);

        mockMvc.perform(post("/api/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDeliveryDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void createDelivery_WithInvalidStatus_ShouldReturn400() throws Exception {
        DeliveryDTO invalid = new DeliveryDTO();
        invalid.setOrderId(1L);
        invalid.setDeliveryAddress("Some address");
        invalid.setStatus("BOGUS");
        invalid.setPharmacyId(1L);

        mockMvc.perform(post("/api/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDeliveriesBatch_ShouldReturn201() throws Exception {
        when(deliveryService.createDeliveriesBatch(anyList())).thenReturn(List.of(testDeliveryDTO));

        mockMvc.perform(post("/api/deliveries/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testDeliveryDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateDelivery_WithValidData_ShouldReturn200() throws Exception {
        when(deliveryService.updateDelivery(eq(1L), any(DeliveryDTO.class))).thenReturn(testDeliveryDTO);

        mockMvc.perform(put("/api/deliveries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDeliveryDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchDelivery_WithValidPatch_ShouldReturn200() throws Exception {
        when(deliveryService.patchDelivery(eq(1L), anyString())).thenReturn(testDeliveryDTO);

        mockMvc.perform(patch("/api/deliveries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"DELIVERED\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchDelivery_WhenInvalidPatch_ShouldReturn400() throws Exception {
        when(deliveryService.patchDelivery(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/deliveries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDelivery_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(deliveryService).deleteDelivery(1L);

        mockMvc.perform(delete("/api/deliveries/1"))
                .andExpect(status().isNoContent());
    }
}

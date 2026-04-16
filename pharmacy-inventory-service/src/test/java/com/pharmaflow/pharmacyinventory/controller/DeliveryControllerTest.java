package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.DeliveryDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.DeliveryService;
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

@WebMvcTest(DeliveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private DeliveryService deliveryService;

    private DeliveryDTO testDeliveryDTO;

    @BeforeEach
    void setUp() {
        testDeliveryDTO = new DeliveryDTO();
        testDeliveryDTO.setId(1L);
        testDeliveryDTO.setOrderId(1L);
        testDeliveryDTO.setDeliveryAddress("Ulica 1, Sarajevo");
        testDeliveryDTO.setStatus("PREPARING");
        testDeliveryDTO.setEstimatedDelivery(LocalDateTime.of(2026, 4, 18, 10, 0));
        testDeliveryDTO.setActualDelivery(null);
        testDeliveryDTO.setPharmacyId(1L);
    }

    @Test
    void getAllDeliveries_ShouldReturn200AndList() throws Exception {
        when(deliveryService.getAllDeliveries()).thenReturn(List.of(testDeliveryDTO));

        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PREPARING"));

        verify(deliveryService, times(1)).getAllDeliveries();
    }

    @Test
    void getDeliveryById_WhenExists_ShouldReturn200() throws Exception {
        when(deliveryService.getDeliveryById(1L)).thenReturn(testDeliveryDTO);

        mockMvc.perform(get("/api/deliveries/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("PREPARING"));

        verify(deliveryService, times(1)).getDeliveryById(1L);
    }

    @Test
    void getDeliveryById_WhenNotExists_ShouldReturn404() throws Exception {
        when(deliveryService.getDeliveryById(999L))
                .thenThrow(new ResourceNotFoundException("Delivery not found with id: 999"));

        mockMvc.perform(get("/api/deliveries/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(deliveryService, times(1)).getDeliveryById(999L);
    }

    @Test
    void getDeliveriesByOrderId_ShouldReturn200AndList() throws Exception {
        when(deliveryService.getDeliveriesByOrderId(1L)).thenReturn(List.of(testDeliveryDTO));

        mockMvc.perform(get("/api/deliveries/order/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId").value(1));

        verify(deliveryService, times(1)).getDeliveriesByOrderId(1L);
    }

    @Test
    void createDelivery_WithValidData_ShouldReturn201() throws Exception {
        when(deliveryService.createDelivery(any(DeliveryDTO.class))).thenReturn(testDeliveryDTO);

        mockMvc.perform(post("/api/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDeliveryDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PREPARING"));

        verify(deliveryService, times(1)).createDelivery(any(DeliveryDTO.class));
    }

    @Test
    void createDelivery_WithInvalidData_ShouldReturn400() throws Exception {
        DeliveryDTO invalidDTO = new DeliveryDTO();
        invalidDTO.setOrderId(1L);
        invalidDTO.setDeliveryAddress("");
        invalidDTO.setStatus("PREPARING");
        invalidDTO.setPharmacyId(1L);

        mockMvc.perform(post("/api/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(deliveryService, never()).createDelivery(any(DeliveryDTO.class));
    }

    @Test
    void updateDelivery_WithValidData_ShouldReturn200() throws Exception {
        DeliveryDTO updatedDTO = new DeliveryDTO();
        updatedDTO.setId(1L);
        updatedDTO.setOrderId(1L);
        updatedDTO.setDeliveryAddress("Ulica 1, Sarajevo");
        updatedDTO.setStatus("IN_TRANSIT");
        updatedDTO.setEstimatedDelivery(LocalDateTime.of(2026, 4, 18, 10, 0));
        updatedDTO.setActualDelivery(null);
        updatedDTO.setPharmacyId(1L);

        when(deliveryService.updateDelivery(eq(1L), any(DeliveryDTO.class))).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/deliveries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        verify(deliveryService, times(1)).updateDelivery(eq(1L), any(DeliveryDTO.class));
    }

    @Test
    void deleteDelivery_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(deliveryService).deleteDelivery(1L);

        mockMvc.perform(delete("/api/deliveries/1"))
                .andExpect(status().isNoContent());

        verify(deliveryService, times(1)).deleteDelivery(1L);
    }
}

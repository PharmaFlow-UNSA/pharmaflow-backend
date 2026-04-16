package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.AutoRefillSubscriptionDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.AutoRefillSubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AutoRefillSubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutoRefillSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private AutoRefillSubscriptionService autoRefillSubscriptionService;

    private AutoRefillSubscriptionDTO subscriptionDTO;

    @BeforeEach
    void setUp() {
        subscriptionDTO = new AutoRefillSubscriptionDTO();
        subscriptionDTO.setId(1L);
        subscriptionDTO.setUserId(1L);
        subscriptionDTO.setProductId(100L);
        subscriptionDTO.setDosagePerDay(1);
        subscriptionDTO.setTabletsPerPackage(30);
        subscriptionDTO.setIntervalDays(30);
        subscriptionDTO.setNextOrderDate(LocalDate.of(2026, 5, 16));
        subscriptionDTO.setStatus("ACTIVE");
        subscriptionDTO.setShippingAddress("Ulica 1, Sarajevo");
        subscriptionDTO.setPrescriptionId(null);
    }

    @Test
    void getAllSubscriptions_ShouldReturn200AndList() throws Exception {
        List<AutoRefillSubscriptionDTO> subscriptions = Arrays.asList(subscriptionDTO);
        when(autoRefillSubscriptionService.getAllSubscriptions()).thenReturn(subscriptions);

        mockMvc.perform(get("/api/auto-refill-subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].productId").value(100))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].shippingAddress").value("Ulica 1, Sarajevo"));

        verify(autoRefillSubscriptionService, times(1)).getAllSubscriptions();
    }

    @Test
    void getSubscriptionById_WhenExists_ShouldReturn200() throws Exception {
        when(autoRefillSubscriptionService.getSubscriptionById(1L)).thenReturn(subscriptionDTO);

        mockMvc.perform(get("/api/auto-refill-subscriptions/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.dosagePerDay").value(1))
                .andExpect(jsonPath("$.tabletsPerPackage").value(30))
                .andExpect(jsonPath("$.intervalDays").value(30))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.shippingAddress").value("Ulica 1, Sarajevo"));

        verify(autoRefillSubscriptionService, times(1)).getSubscriptionById(1L);
    }

    @Test
    void getSubscriptionById_WhenNotExists_ShouldReturn404() throws Exception {
        when(autoRefillSubscriptionService.getSubscriptionById(999L))
                .thenThrow(new ResourceNotFoundException("Subscription not found with id: 999"));

        mockMvc.perform(get("/api/auto-refill-subscriptions/999"))
                .andExpect(status().isNotFound());

        verify(autoRefillSubscriptionService, times(1)).getSubscriptionById(999L);
    }

    @Test
    void getSubscriptionsByUserId_ShouldReturn200AndList() throws Exception {
        List<AutoRefillSubscriptionDTO> subscriptions = Arrays.asList(subscriptionDTO);
        when(autoRefillSubscriptionService.getSubscriptionsByUserId(1L)).thenReturn(subscriptions);

        mockMvc.perform(get("/api/auto-refill-subscriptions/user/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(autoRefillSubscriptionService, times(1)).getSubscriptionsByUserId(1L);
    }

    @Test
    void createSubscription_WithValidData_ShouldReturn201() throws Exception {
        when(autoRefillSubscriptionService.createSubscription(any(AutoRefillSubscriptionDTO.class)))
                .thenReturn(subscriptionDTO);

        mockMvc.perform(post("/api/auto-refill-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscriptionDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.shippingAddress").value("Ulica 1, Sarajevo"));

        verify(autoRefillSubscriptionService, times(1)).createSubscription(any(AutoRefillSubscriptionDTO.class));
    }

    @Test
    void createSubscription_WithInvalidData_ShouldReturn400() throws Exception {
        AutoRefillSubscriptionDTO invalidDTO = new AutoRefillSubscriptionDTO();
        invalidDTO.setUserId(1L);
        invalidDTO.setProductId(100L);
        invalidDTO.setDosagePerDay(1);
        invalidDTO.setTabletsPerPackage(30);
        invalidDTO.setStatus("ACTIVE");
        invalidDTO.setShippingAddress("");

        mockMvc.perform(post("/api/auto-refill-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(autoRefillSubscriptionService, never()).createSubscription(any(AutoRefillSubscriptionDTO.class));
    }

    @Test
    void updateSubscription_WithValidData_ShouldReturn200() throws Exception {
        when(autoRefillSubscriptionService.updateSubscription(eq(1L), any(AutoRefillSubscriptionDTO.class)))
                .thenReturn(subscriptionDTO);

        mockMvc.perform(put("/api/auto-refill-subscriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscriptionDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(autoRefillSubscriptionService, times(1)).updateSubscription(eq(1L), any(AutoRefillSubscriptionDTO.class));
    }

    @Test
    void deleteSubscription_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(autoRefillSubscriptionService).deleteSubscription(1L);

        mockMvc.perform(delete("/api/auto-refill-subscriptions/1"))
                .andExpect(status().isNoContent());

        verify(autoRefillSubscriptionService, times(1)).deleteSubscription(1L);
    }
}

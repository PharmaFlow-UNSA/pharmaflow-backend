package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.AutoRefillSubscriptionDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.AutoRefillSubscriptionService;
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

import java.time.LocalDate;
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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private AutoRefillSubscriptionService autoRefillSubscriptionService;

    private AutoRefillSubscriptionDTO testDTO;

    @BeforeEach
    void setUp() {
        testDTO = new AutoRefillSubscriptionDTO();
        testDTO.setId(1L);
        testDTO.setUserId(10L);
        testDTO.setProductId(100L);
        testDTO.setDosagePerDay(2);
        testDTO.setTabletsPerPackage(60);
        testDTO.setIntervalDays(30);
        testDTO.setNextOrderDate(LocalDate.of(2026, 6, 1));
        testDTO.setStatus("ACTIVE");
        testDTO.setShippingAddress("Marsala Tita 10, Sarajevo");
    }

    @Test
    void getSubscriptions_ShouldReturn200AndPagedResult() throws Exception {
        Page<AutoRefillSubscriptionDTO> page = new PageImpl<>(List.of(testDTO));
        when(autoRefillSubscriptionService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/auto-refill-subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getSubscriptionById_WhenExists_ShouldReturn200() throws Exception {
        when(autoRefillSubscriptionService.getSubscriptionById(1L)).thenReturn(testDTO);

        mockMvc.perform(get("/api/auto-refill-subscriptions/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getSubscriptionById_WhenNotExists_ShouldReturn404() throws Exception {
        when(autoRefillSubscriptionService.getSubscriptionById(999L))
                .thenThrow(new ResourceNotFoundException("Subscription not found with id: 999"));

        mockMvc.perform(get("/api/auto-refill-subscriptions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSubscriptionsByUserId_ShouldReturn200() throws Exception {
        when(autoRefillSubscriptionService.getSubscriptionsByUserId(10L)).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/auto-refill-subscriptions/user/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getSubscriptionsByStatus_ShouldReturn200() throws Exception {
        when(autoRefillSubscriptionService.getSubscriptionsByStatus("ACTIVE")).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/auto-refill-subscriptions/status/ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    void createSubscription_WithValidData_ShouldReturn201() throws Exception {
        when(autoRefillSubscriptionService.createSubscription(any(AutoRefillSubscriptionDTO.class))).thenReturn(testDTO);

        mockMvc.perform(post("/api/auto-refill-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void createSubscription_WithInvalidStatus_ShouldReturn400() throws Exception {
        AutoRefillSubscriptionDTO invalid = new AutoRefillSubscriptionDTO();
        invalid.setUserId(10L);
        invalid.setProductId(100L);
        invalid.setDosagePerDay(2);
        invalid.setTabletsPerPackage(60);
        invalid.setStatus("BOGUS");
        invalid.setShippingAddress("addr...");

        mockMvc.perform(post("/api/auto-refill-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSubscriptionsBatch_ShouldReturn201() throws Exception {
        when(autoRefillSubscriptionService.createSubscriptionsBatch(anyList())).thenReturn(List.of(testDTO));

        mockMvc.perform(post("/api/auto-refill-subscriptions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateSubscription_WithValidData_ShouldReturn200() throws Exception {
        when(autoRefillSubscriptionService.updateSubscription(eq(1L), any(AutoRefillSubscriptionDTO.class))).thenReturn(testDTO);

        mockMvc.perform(put("/api/auto-refill-subscriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchSubscription_WithValidPatch_ShouldReturn200() throws Exception {
        when(autoRefillSubscriptionService.patchSubscription(eq(1L), anyString())).thenReturn(testDTO);

        mockMvc.perform(patch("/api/auto-refill-subscriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"PAUSED\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchSubscription_WithInvalidPatch_ShouldReturn400() throws Exception {
        when(autoRefillSubscriptionService.patchSubscription(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/auto-refill-subscriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteSubscription_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(autoRefillSubscriptionService).deleteSubscription(1L);

        mockMvc.perform(delete("/api/auto-refill-subscriptions/1"))
                .andExpect(status().isNoContent());
    }
}

package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.PaymentService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private PaymentService paymentService;

    private PaymentDTO testDTO;

    @BeforeEach
    void setUp() {
        testDTO = new PaymentDTO();
        testDTO.setId(1L);
        testDTO.setAmount(new BigDecimal("99.99"));
        testDTO.setMethod("CARD");
        testDTO.setStatus("COMPLETED");
        testDTO.setTransactionId("TX-001");
    }

    @Test
    void getPayments_ShouldReturn200AndPagedResult() throws Exception {
        Page<PaymentDTO> page = new PageImpl<>(List.of(testDTO));
        when(paymentService.findAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getPaymentById_WhenExists_ShouldReturn200() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(testDTO);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPaymentById_WhenNotExists_ShouldReturn404() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 999"));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPayment_WithValidData_ShouldReturn201() throws Exception {
        when(paymentService.createPayment(any(PaymentDTO.class))).thenReturn(testDTO);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void createPayment_WithInvalidStatus_ShouldReturn400() throws Exception {
        PaymentDTO invalid = new PaymentDTO();
        invalid.setAmount(new BigDecimal("99.99"));
        invalid.setMethod("CARD");
        invalid.setStatus("BOGUS");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPaymentsBatch_ShouldReturn201() throws Exception {
        when(paymentService.createPaymentsBatch(anyList())).thenReturn(List.of(testDTO));

        mockMvc.perform(post("/api/payments/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    void updatePayment_WithValidData_ShouldReturn200() throws Exception {
        when(paymentService.updatePayment(eq(1L), any(PaymentDTO.class))).thenReturn(testDTO);

        mockMvc.perform(put("/api/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchPayment_WithValidPatch_ShouldReturn200() throws Exception {
        when(paymentService.patchPayment(eq(1L), anyString())).thenReturn(testDTO);

        mockMvc.perform(patch("/api/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"REFUNDED\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchPayment_WithInvalidPatch_ShouldReturn400() throws Exception {
        when(paymentService.patchPayment(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletePayment_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(paymentService).deletePayment(1L);

        mockMvc.perform(delete("/api/payments/1"))
                .andExpect(status().isNoContent());
    }
}

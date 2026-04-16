package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private PaymentService paymentService;

    private PaymentDTO paymentDTO;

    @BeforeEach
    void setUp() {
        paymentDTO = new PaymentDTO();
        paymentDTO.setId(1L);
        paymentDTO.setAmount(BigDecimal.valueOf(25.00));
        paymentDTO.setMethod("CARD");
        paymentDTO.setStatus("PENDING");
        paymentDTO.setTransactionId("TXN-00001");
        paymentDTO.setPaidAt(null);
    }

    @Test
    void getAllPayments_ShouldReturn200AndList() throws Exception {
        List<PaymentDTO> payments = Arrays.asList(paymentDTO);
        when(paymentService.getAllPayments()).thenReturn(payments);

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(25.00))
                .andExpect(jsonPath("$[0].method").value("CARD"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].transactionId").value("TXN-00001"));

        verify(paymentService, times(1)).getAllPayments();
    }

    @Test
    void getPaymentById_WhenExists_ShouldReturn200() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(paymentDTO);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.transactionId").value("TXN-00001"));

        verify(paymentService, times(1)).getPaymentById(1L);
    }

    @Test
    void getPaymentById_WhenNotExists_ShouldReturn404() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 999"));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());

        verify(paymentService, times(1)).getPaymentById(999L);
    }

    @Test
    void createPayment_WithValidData_ShouldReturn201() throws Exception {
        when(paymentService.createPayment(any(PaymentDTO.class))).thenReturn(paymentDTO);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService, times(1)).createPayment(any(PaymentDTO.class));
    }

    @Test
    void createPayment_WithInvalidData_ShouldReturn400() throws Exception {
        PaymentDTO invalidDTO = new PaymentDTO();
        invalidDTO.setAmount(BigDecimal.valueOf(25.00));
        invalidDTO.setMethod("");
        invalidDTO.setStatus("PENDING");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).createPayment(any(PaymentDTO.class));
    }

    @Test
    void updatePayment_WithValidData_ShouldReturn200() throws Exception {
        when(paymentService.updatePayment(eq(1L), any(PaymentDTO.class))).thenReturn(paymentDTO);

        mockMvc.perform(put("/api/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService, times(1)).updatePayment(eq(1L), any(PaymentDTO.class));
    }

    @Test
    void deletePayment_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(paymentService).deletePayment(1L);

        mockMvc.perform(delete("/api/payments/1"))
                .andExpect(status().isNoContent());

        verify(paymentService, times(1)).deletePayment(1L);
    }
}

package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Payment;
import com.pharmaflow.orderprescription.repositories.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private PaymentService paymentService;

    private Payment testPayment;
    private PaymentDTO testDTO;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, modelMapper, objectMapper, validator);

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setMethod("CARD");
        testPayment.setStatus("COMPLETED");

        testDTO = new PaymentDTO();
        testDTO.setAmount(new BigDecimal("99.99"));
        testDTO.setMethod("CARD");
        testDTO.setStatus("COMPLETED");

        lenient().when(modelMapper.map(any(Payment.class), eq(PaymentDTO.class))).thenReturn(testDTO);
        lenient().when(modelMapper.map(any(PaymentDTO.class), eq(Payment.class))).thenReturn(testPayment);
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(testPayment));
        when(paymentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PaymentDTO> result = paymentService.findAll(null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPaymentById_WhenExists_ShouldReturn() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        PaymentDTO result = paymentService.getPaymentById(1L);

        assertNotNull(result);
    }

    @Test
    void getPaymentById_WhenNotExists_ShouldThrow() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentById(99L));
    }

    @Test
    void createPayment_ShouldSave() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        PaymentDTO result = paymentService.createPayment(testDTO);

        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPaymentsBatch_ShouldSaveAll() {
        when(paymentRepository.saveAll(any())).thenReturn(List.of(testPayment));

        List<PaymentDTO> result = paymentService.createPaymentsBatch(List.of(testDTO));

        assertEquals(1, result.size());
    }

    @Test
    void updatePayment_WhenExists_ShouldUpdate() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        PaymentDTO result = paymentService.updatePayment(1L, testDTO);

        assertNotNull(result);
    }

    @Test
    void updatePayment_WhenNotExists_ShouldThrow() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.updatePayment(99L, testDTO));
    }

    @Test
    void patchPayment_WithValidPatch_ShouldUpdate() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        PaymentDTO result = paymentService.patchPayment(1L,
                "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"REFUNDED\"}]");

        assertNotNull(result);
    }

    @Test
    void patchPayment_WithMalformedPatch_ShouldThrow() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        assertThrows(PatchOperationException.class,
                () -> paymentService.patchPayment(1L, "junk"));
    }

    @Test
    void deletePayment_WhenExists_ShouldDelete() {
        when(paymentRepository.existsById(1L)).thenReturn(true);

        paymentService.deletePayment(1L);

        verify(paymentRepository).deleteById(1L);
    }

    @Test
    void deletePayment_WhenNotExists_ShouldThrow() {
        when(paymentRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.deletePayment(99L));
    }
}

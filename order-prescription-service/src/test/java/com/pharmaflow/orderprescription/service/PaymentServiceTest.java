package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Payment;
import com.pharmaflow.orderprescription.repositories.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private PaymentDTO testPaymentDTO;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setAmount(BigDecimal.valueOf(25.00));
        testPayment.setMethod("CARD");
        testPayment.setStatus("PENDING");
        testPayment.setTransactionId("TXN-00001");
        testPayment.setPaidAt(null);

        testPaymentDTO = new PaymentDTO();
        testPaymentDTO.setId(1L);
        testPaymentDTO.setAmount(BigDecimal.valueOf(25.00));
        testPaymentDTO.setMethod("CARD");
        testPaymentDTO.setStatus("PENDING");
        testPaymentDTO.setTransactionId("TXN-00001");
        testPaymentDTO.setPaidAt(null);
    }

    @Test
    void getAllPayments_WhenPaymentsExist_ShouldReturnPaymentList() {
        when(paymentRepository.findAll()).thenReturn(List.of(testPayment));
        when(modelMapper.map(any(Payment.class), eq(PaymentDTO.class))).thenReturn(testPaymentDTO);

        List<PaymentDTO> result = paymentService.getAllPayments();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPaymentDTO.getId(), result.get(0).getId());
        assertEquals(testPaymentDTO.getAmount(), result.get(0).getAmount());
        verify(paymentRepository, times(1)).findAll();
        verify(modelMapper, times(1)).map(any(Payment.class), eq(PaymentDTO.class));
    }

    @Test
    void getPaymentById_WhenPaymentExists_ShouldReturnPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(modelMapper.map(any(Payment.class), eq(PaymentDTO.class))).thenReturn(testPaymentDTO);

        PaymentDTO result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(testPaymentDTO.getId(), result.getId());
        assertEquals(testPaymentDTO.getAmount(), result.getAmount());
        assertEquals(testPaymentDTO.getMethod(), result.getMethod());
        assertEquals(testPaymentDTO.getStatus(), result.getStatus());
        assertEquals(testPaymentDTO.getTransactionId(), result.getTransactionId());
        verify(paymentRepository, times(1)).findById(1L);
        verify(modelMapper, times(1)).map(any(Payment.class), eq(PaymentDTO.class));
    }

    @Test
    void getPaymentById_WhenPaymentNotExists_ShouldThrowResourceNotFoundException() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById(99L));
        verify(paymentRepository, times(1)).findById(99L);
    }

    @Test
    void createPayment_WhenValidInput_ShouldReturnCreatedPayment() {
        when(modelMapper.map(any(PaymentDTO.class), eq(Payment.class))).thenReturn(testPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(modelMapper.map(any(Payment.class), eq(PaymentDTO.class))).thenReturn(testPaymentDTO);

        PaymentDTO result = paymentService.createPayment(testPaymentDTO);

        assertNotNull(result);
        assertEquals(testPaymentDTO.getId(), result.getId());
        assertEquals(testPaymentDTO.getAmount(), result.getAmount());
        assertEquals(testPaymentDTO.getMethod(), result.getMethod());
        assertEquals(testPaymentDTO.getStatus(), result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void updatePayment_WhenPaymentExists_ShouldReturnUpdatedPayment() {
        PaymentDTO updateDTO = new PaymentDTO();
        updateDTO.setAmount(BigDecimal.valueOf(30.00));
        updateDTO.setMethod("TRANSFER");
        updateDTO.setStatus("COMPLETED");
        updateDTO.setTransactionId("TXN-00002");
        updateDTO.setPaidAt(null);

        PaymentDTO updatedPaymentDTO = new PaymentDTO();
        updatedPaymentDTO.setId(1L);
        updatedPaymentDTO.setAmount(BigDecimal.valueOf(30.00));
        updatedPaymentDTO.setMethod("TRANSFER");
        updatedPaymentDTO.setStatus("COMPLETED");
        updatedPaymentDTO.setTransactionId("TXN-00002");
        updatedPaymentDTO.setPaidAt(null);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(modelMapper.map(any(Payment.class), eq(PaymentDTO.class))).thenReturn(updatedPaymentDTO);

        PaymentDTO result = paymentService.updatePayment(1L, updateDTO);

        assertNotNull(result);
        assertEquals(updatedPaymentDTO.getAmount(), result.getAmount());
        assertEquals(updatedPaymentDTO.getMethod(), result.getMethod());
        assertEquals(updatedPaymentDTO.getStatus(), result.getStatus());
        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void updatePayment_WhenPaymentNotExists_ShouldThrowResourceNotFoundException() {
        PaymentDTO updateDTO = new PaymentDTO();

        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.updatePayment(99L, updateDTO));
        verify(paymentRepository, times(1)).findById(99L);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void deletePayment_WhenPaymentExists_ShouldDeleteSuccessfully() {
        when(paymentRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> paymentService.deletePayment(1L));
        verify(paymentRepository, times(1)).existsById(1L);
        verify(paymentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deletePayment_WhenPaymentNotExists_ShouldThrowResourceNotFoundException() {
        when(paymentRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> paymentService.deletePayment(99L));
        verify(paymentRepository, times(1)).existsById(99L);
        verify(paymentRepository, never()).deleteById(any());
    }
}

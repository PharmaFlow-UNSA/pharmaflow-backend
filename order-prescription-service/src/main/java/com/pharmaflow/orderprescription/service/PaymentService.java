package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Payment;
import com.pharmaflow.orderprescription.repositories.PaymentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

    public PaymentService(PaymentRepository paymentRepository, ModelMapper modelMapper) {
        this.paymentRepository = paymentRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(payment -> modelMapper.map(payment, PaymentDTO.class))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return modelMapper.map(payment, PaymentDTO.class);
    }

    @Transactional
    public PaymentDTO createPayment(PaymentDTO dto) {
        Payment payment = modelMapper.map(dto, Payment.class);
        Payment saved = paymentRepository.save(payment);
        return modelMapper.map(saved, PaymentDTO.class);
    }

    @Transactional
    public PaymentDTO updatePayment(Long id, PaymentDTO dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        payment.setAmount(dto.getAmount());
        payment.setMethod(dto.getMethod());
        payment.setStatus(dto.getStatus());
        payment.setTransactionId(dto.getTransactionId());
        payment.setPaidAt(dto.getPaidAt());

        Payment saved = paymentRepository.save(payment);
        return modelMapper.map(saved, PaymentDTO.class);
    }

    @Transactional
    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payment not found with id: " + id);
        }
        paymentRepository.deleteById(id);
    }
}

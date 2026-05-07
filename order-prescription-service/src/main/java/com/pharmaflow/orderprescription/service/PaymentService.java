package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Payment;
import com.pharmaflow.orderprescription.repositories.PaymentRepository;
import com.pharmaflow.orderprescription.specifications.PaymentSpecs;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PaymentService(PaymentRepository paymentRepository,
                          ModelMapper modelMapper,
                          ObjectMapper objectMapper,
                          Validator validator) {
        this.paymentRepository = paymentRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<PaymentDTO> findAll(String status, String method, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Payment> spec = Specification
                .where(PaymentSpecs.hasStatus(status))
                .and(PaymentSpecs.hasMethod(method));

        Page<PaymentDTO> result = paymentRepository.findAll(spec, pageable)
                .map(p -> modelMapper.map(p, PaymentDTO.class));

        log.info("Payment findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
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
        log.info("Payment created with id: {}", saved.getId());
        return modelMapper.map(saved, PaymentDTO.class);
    }

    @Transactional
    public List<PaymentDTO> createPaymentsBatch(List<PaymentDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<Payment> payments = new ArrayList<>();

        for (PaymentDTO dto : dtos) {
            payments.add(modelMapper.map(dto, Payment.class));
        }

        List<Payment> saved = paymentRepository.saveAll(payments);
        log.info("Batch created {} payments in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream()
                .map(p -> modelMapper.map(p, PaymentDTO.class))
                .toList();
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
        log.info("Payment updated with id: {}", saved.getId());
        return modelMapper.map(saved, PaymentDTO.class);
    }

    @Transactional
    public PaymentDTO patchPayment(Long id, String patchDocument) {
        try {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

            // Patch the DTO so Jakarta Bean Validation can be re-applied
            PaymentDTO currentDTO = modelMapper.map(payment, PaymentDTO.class);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            PaymentDTO patchedDTO = objectMapper.treeToValue(patchedJson, PaymentDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<PaymentDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            payment.setAmount(patchedDTO.getAmount());
            payment.setMethod(patchedDTO.getMethod());
            payment.setStatus(patchedDTO.getStatus());
            payment.setTransactionId(patchedDTO.getTransactionId());
            payment.setPaidAt(patchedDTO.getPaidAt());

            Payment saved = paymentRepository.save(payment);
            log.info("Payment patched with id: {}", saved.getId());
            return modelMapper.map(saved, PaymentDTO.class);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payment not found with id: " + id);
        }
        paymentRepository.deleteById(id);
        log.info("Payment deleted with id: {}", id);
    }
}

package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import com.pharmaflow.orderprescription.specifications.PrescriptionSpecs;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               ModelMapper modelMapper,
                               ObjectMapper objectMapper,
                               Validator validator) {
        this.prescriptionRepository = prescriptionRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionDTO> findAll(Long userId, String status, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Prescription> spec = Specification
                .where(PrescriptionSpecs.hasUserId(userId))
                .and(PrescriptionSpecs.hasStatus(status));

        Page<PrescriptionDTO> result = prescriptionRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("Prescription findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    @Transactional(readOnly = true)
    public PrescriptionDTO getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));
        return convertToDTO(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> getPrescriptionsByUserId(Long userId) {
        return prescriptionRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> getPrescriptionsByStatus(String status) {
        return prescriptionRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public PrescriptionDTO createPrescription(PrescriptionCreateDTO dto) {
        Prescription prescription = new Prescription();
        prescription.setUserId(dto.getUserId());
        prescription.setImageUrl(dto.getImageUrl());
        prescription.setStatus("PENDING");
        prescription.setUploadedAt(LocalDateTime.now());

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Prescription created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<PrescriptionDTO> createPrescriptionsBatch(List<PrescriptionCreateDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<Prescription> prescriptions = new ArrayList<>();

        for (PrescriptionCreateDTO dto : dtos) {
            Prescription p = new Prescription();
            p.setUserId(dto.getUserId());
            p.setImageUrl(dto.getImageUrl());
            p.setStatus("PENDING");
            p.setUploadedAt(LocalDateTime.now());
            prescriptions.add(p);
        }

        List<Prescription> saved = prescriptionRepository.saveAll(prescriptions);
        log.info("Batch created {} prescriptions in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public PrescriptionDTO updatePrescription(Long id, PrescriptionDTO dto) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));

        prescription.setUserId(dto.getUserId());
        prescription.setImageUrl(dto.getImageUrl());
        prescription.setStatus(dto.getStatus());
        prescription.setReviewerNotes(dto.getReviewerNotes());

        if (!"PENDING".equals(dto.getStatus())) {
            prescription.setReviewedAt(LocalDateTime.now());
        }

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Prescription updated with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public PrescriptionDTO patchPrescription(Long id, String patchDocument) {
        try {
            Prescription prescription = prescriptionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));

            PrescriptionDTO currentDTO = convertToDTO(prescription);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            PrescriptionDTO patchedDTO = objectMapper.treeToValue(patchedJson, PrescriptionDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<PrescriptionDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            prescription.setUserId(patchedDTO.getUserId());
            prescription.setImageUrl(patchedDTO.getImageUrl());
            prescription.setStatus(patchedDTO.getStatus());
            prescription.setReviewerNotes(patchedDTO.getReviewerNotes());

            if (!"PENDING".equals(patchedDTO.getStatus())) {
                prescription.setReviewedAt(LocalDateTime.now());
            }

            Prescription saved = prescriptionRepository.save(prescription);
            log.info("Prescription patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deletePrescription(Long id) {
        if (!prescriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prescription not found with id: " + id);
        }
        prescriptionRepository.deleteById(id);
        log.info("Prescription deleted with id: {}", id);
    }

    private PrescriptionDTO convertToDTO(Prescription prescription) {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setId(prescription.getId());
        dto.setUserId(prescription.getUserId());
        dto.setImageUrl(prescription.getImageUrl());
        dto.setStatus(prescription.getStatus());
        dto.setUploadedAt(prescription.getUploadedAt());
        dto.setReviewedAt(prescription.getReviewedAt());
        dto.setReviewerNotes(prescription.getReviewerNotes());

        dto.setOrderIds(
                prescription.getOrders() != null
                        ? prescription.getOrders().stream().map(Order::getId).toList()
                        : Collections.emptyList()
        );

        dto.setAutoRefillSubscriptionIds(
                prescription.getAutoRefillSubscriptions() != null
                        ? prescription.getAutoRefillSubscriptions().stream().map(AutoRefillSubscription::getId).toList()
                        : Collections.emptyList()
        );

        return dto;
    }
}

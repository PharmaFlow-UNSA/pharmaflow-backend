package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.orderprescription.dto.AutoRefillSubscriptionDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.AutoRefillSubscriptionRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import com.pharmaflow.orderprescription.specifications.AutoRefillSubscriptionSpecs;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AutoRefillSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(AutoRefillSubscriptionService.class);

    private final AutoRefillSubscriptionRepository autoRefillSubscriptionRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AutoRefillSubscriptionService(AutoRefillSubscriptionRepository autoRefillSubscriptionRepository,
                                         PrescriptionRepository prescriptionRepository,
                                         ModelMapper modelMapper,
                                         ObjectMapper objectMapper,
                                         Validator validator) {
        this.autoRefillSubscriptionRepository = autoRefillSubscriptionRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<AutoRefillSubscriptionDTO> findAll(Long userId, String status, Long productId, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<AutoRefillSubscription> spec = Specification
                .where(AutoRefillSubscriptionSpecs.hasUserId(userId))
                .and(AutoRefillSubscriptionSpecs.hasStatus(status))
                .and(AutoRefillSubscriptionSpecs.hasProductId(productId));

        Page<AutoRefillSubscriptionDTO> result = autoRefillSubscriptionRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("AutoRefillSubscription findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    @Transactional(readOnly = true)
    public AutoRefillSubscriptionDTO getSubscriptionById(Long id) {
        AutoRefillSubscription subscription = autoRefillSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));
        return convertToDTO(subscription);
    }

    @Transactional(readOnly = true)
    public List<AutoRefillSubscriptionDTO> getSubscriptionsByUserId(Long userId) {
        return autoRefillSubscriptionRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AutoRefillSubscriptionDTO> getSubscriptionsByStatus(String status) {
        return autoRefillSubscriptionRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public AutoRefillSubscriptionDTO createSubscription(AutoRefillSubscriptionDTO dto) {
        AutoRefillSubscription subscription = buildFromDTO(dto);
        AutoRefillSubscription saved = autoRefillSubscriptionRepository.save(subscription);
        log.info("Subscription created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<AutoRefillSubscriptionDTO> createSubscriptionsBatch(List<AutoRefillSubscriptionDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<AutoRefillSubscription> subscriptions = new ArrayList<>();

        for (AutoRefillSubscriptionDTO dto : dtos) {
            subscriptions.add(buildFromDTO(dto));
        }

        List<AutoRefillSubscription> saved = autoRefillSubscriptionRepository.saveAll(subscriptions);
        log.info("Batch created {} subscriptions in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public AutoRefillSubscriptionDTO updateSubscription(Long id, AutoRefillSubscriptionDTO dto) {
        AutoRefillSubscription subscription = autoRefillSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        subscription.setUserId(dto.getUserId());
        subscription.setProductId(dto.getProductId());
        subscription.setStatus(dto.getStatus());
        subscription.setShippingAddress(dto.getShippingAddress());

        if (dto.getDosagePerDay() != null && dto.getTabletsPerPackage() != null) {
            if (dto.getDosagePerDay() <= 0) {
                throw new IllegalArgumentException("Dosage per day must be at least 1");
            }
            subscription.setDosagePerDay(dto.getDosagePerDay());
            subscription.setTabletsPerPackage(dto.getTabletsPerPackage());

            int intervalDays = dto.getTabletsPerPackage() / dto.getDosagePerDay();
            subscription.setIntervalDays(intervalDays);
            subscription.setNextOrderDate(LocalDate.now().plusDays(intervalDays));
        }

        if (dto.getPrescriptionId() != null) {
            Prescription prescription = prescriptionRepository.findById(dto.getPrescriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prescription not found with id: " + dto.getPrescriptionId()));
            subscription.setPrescription(prescription);
        }

        AutoRefillSubscription saved = autoRefillSubscriptionRepository.save(subscription);
        log.info("Subscription updated with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public AutoRefillSubscriptionDTO patchSubscription(Long id, String patchDocument) {
        try {
            AutoRefillSubscription subscription = autoRefillSubscriptionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

            AutoRefillSubscriptionDTO currentDTO = convertToDTO(subscription);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            AutoRefillSubscriptionDTO patchedDTO = objectMapper.treeToValue(patchedJson, AutoRefillSubscriptionDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<AutoRefillSubscriptionDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            subscription.setUserId(patchedDTO.getUserId());
            subscription.setProductId(patchedDTO.getProductId());
            subscription.setStatus(patchedDTO.getStatus());
            subscription.setShippingAddress(patchedDTO.getShippingAddress());

            if (patchedDTO.getDosagePerDay() != null && patchedDTO.getTabletsPerPackage() != null) {
                if (patchedDTO.getDosagePerDay() <= 0) {
                    throw new IllegalArgumentException("Dosage per day must be at least 1");
                }
                subscription.setDosagePerDay(patchedDTO.getDosagePerDay());
                subscription.setTabletsPerPackage(patchedDTO.getTabletsPerPackage());
                int intervalDays = patchedDTO.getTabletsPerPackage() / patchedDTO.getDosagePerDay();
                subscription.setIntervalDays(intervalDays);
            }

            if (patchedDTO.getPrescriptionId() != null
                    && (subscription.getPrescription() == null
                        || !patchedDTO.getPrescriptionId().equals(subscription.getPrescription().getId()))) {
                Prescription prescription = prescriptionRepository.findById(patchedDTO.getPrescriptionId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Prescription not found with id: " + patchedDTO.getPrescriptionId()));
                subscription.setPrescription(prescription);
            }

            AutoRefillSubscription saved = autoRefillSubscriptionRepository.save(subscription);
            log.info("Subscription patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteSubscription(Long id) {
        if (!autoRefillSubscriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subscription not found with id: " + id);
        }
        autoRefillSubscriptionRepository.deleteById(id);
        log.info("Subscription deleted with id: {}", id);
    }

    private AutoRefillSubscription buildFromDTO(AutoRefillSubscriptionDTO dto) {
        AutoRefillSubscription subscription = new AutoRefillSubscription();
        subscription.setUserId(dto.getUserId());
        subscription.setProductId(dto.getProductId());
        subscription.setDosagePerDay(dto.getDosagePerDay());
        subscription.setTabletsPerPackage(dto.getTabletsPerPackage());

        if (dto.getDosagePerDay() == null || dto.getDosagePerDay() <= 0) {
            throw new IllegalArgumentException("Dosage per day must be at least 1");
        }
        int intervalDays = dto.getTabletsPerPackage() / dto.getDosagePerDay();
        subscription.setIntervalDays(intervalDays);
        subscription.setNextOrderDate(LocalDate.now().plusDays(intervalDays));

        subscription.setStatus(dto.getStatus());
        subscription.setShippingAddress(dto.getShippingAddress());

        if (dto.getPrescriptionId() != null) {
            Prescription prescription = prescriptionRepository.findById(dto.getPrescriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prescription not found with id: " + dto.getPrescriptionId()));
            subscription.setPrescription(prescription);
        }
        return subscription;
    }

    private AutoRefillSubscriptionDTO convertToDTO(AutoRefillSubscription subscription) {
        AutoRefillSubscriptionDTO dto = new AutoRefillSubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setUserId(subscription.getUserId());
        dto.setProductId(subscription.getProductId());
        dto.setDosagePerDay(subscription.getDosagePerDay());
        dto.setTabletsPerPackage(subscription.getTabletsPerPackage());
        dto.setIntervalDays(subscription.getIntervalDays());
        dto.setNextOrderDate(subscription.getNextOrderDate());
        dto.setStatus(subscription.getStatus());
        dto.setShippingAddress(subscription.getShippingAddress());

        if (subscription.getPrescription() != null) {
            dto.setPrescriptionId(subscription.getPrescription().getId());
        }

        return dto;
    }
}

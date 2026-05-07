package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.pharmacyinventory.dto.DeliveryDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Delivery;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.DeliveryRepository;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import com.pharmaflow.pharmacyinventory.specifications.DeliverySpecs;
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
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           PharmacyRepository pharmacyRepository,
                           ModelMapper modelMapper,
                           ObjectMapper objectMapper,
                           Validator validator) {
        this.deliveryRepository = deliveryRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<DeliveryDTO> findAll(String status, Long orderId, Long pharmacyId, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Delivery> spec = Specification
                .where(DeliverySpecs.hasStatus(status))
                .and(DeliverySpecs.hasOrderId(orderId))
                .and(DeliverySpecs.hasPharmacyId(pharmacyId));

        Page<DeliveryDTO> result = deliveryRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("Delivery findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryById(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));
        return convertToDTO(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesByPharmacyId(Long pharmacyId) {
        return deliveryRepository.findByPharmacyId(pharmacyId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesByStatus(String status) {
        return deliveryRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public DeliveryDTO createDelivery(DeliveryDTO deliveryDTO) {
        Pharmacy pharmacy = pharmacyRepository.findById(deliveryDTO.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pharmacy not found with id: " + deliveryDTO.getPharmacyId()));

        Delivery delivery = new Delivery();
        delivery.setOrderId(deliveryDTO.getOrderId());
        delivery.setDeliveryAddress(deliveryDTO.getDeliveryAddress());
        delivery.setStatus(deliveryDTO.getStatus());
        delivery.setEstimatedDelivery(deliveryDTO.getEstimatedDelivery());
        delivery.setActualDelivery(deliveryDTO.getActualDelivery());
        delivery.setPharmacy(pharmacy);

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<DeliveryDTO> createDeliveriesBatch(List<DeliveryDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<Delivery> deliveries = new ArrayList<>();

        for (DeliveryDTO dto : dtos) {
            Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pharmacy not found with id: " + dto.getPharmacyId()));

            Delivery delivery = new Delivery();
            delivery.setOrderId(dto.getOrderId());
            delivery.setDeliveryAddress(dto.getDeliveryAddress());
            delivery.setStatus(dto.getStatus());
            delivery.setEstimatedDelivery(dto.getEstimatedDelivery());
            delivery.setActualDelivery(dto.getActualDelivery());
            delivery.setPharmacy(pharmacy);

            deliveries.add(delivery);
        }

        List<Delivery> saved = deliveryRepository.saveAll(deliveries);
        log.info("Batch created {} deliveries in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public DeliveryDTO updateDelivery(Long id, DeliveryDTO deliveryDTO) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));

        delivery.setOrderId(deliveryDTO.getOrderId());
        delivery.setDeliveryAddress(deliveryDTO.getDeliveryAddress());
        delivery.setStatus(deliveryDTO.getStatus());
        delivery.setEstimatedDelivery(deliveryDTO.getEstimatedDelivery());
        delivery.setActualDelivery(deliveryDTO.getActualDelivery());

        Delivery updated = deliveryRepository.save(delivery);
        log.info("Delivery updated with id: {}", updated.getId());
        return convertToDTO(updated);
    }

    @Transactional
    public DeliveryDTO patchDelivery(Long id, String patchDocument) {
        try {
            Delivery delivery = deliveryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));

            DeliveryDTO currentDTO = convertToDTO(delivery);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            DeliveryDTO patchedDTO = objectMapper.treeToValue(patchedJson, DeliveryDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<DeliveryDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            delivery.setOrderId(patchedDTO.getOrderId());
            delivery.setDeliveryAddress(patchedDTO.getDeliveryAddress());
            delivery.setStatus(patchedDTO.getStatus());
            delivery.setEstimatedDelivery(patchedDTO.getEstimatedDelivery());
            delivery.setActualDelivery(patchedDTO.getActualDelivery());

            if (patchedDTO.getPharmacyId() != null
                    && !patchedDTO.getPharmacyId().equals(delivery.getPharmacy().getId())) {
                Pharmacy pharmacy = pharmacyRepository.findById(patchedDTO.getPharmacyId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Pharmacy not found with id: " + patchedDTO.getPharmacyId()));
                delivery.setPharmacy(pharmacy);
            }

            Delivery saved = deliveryRepository.save(delivery);
            log.info("Delivery patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteDelivery(Long id) {
        if (!deliveryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Delivery not found with id: " + id);
        }
        deliveryRepository.deleteById(id);
        log.info("Delivery deleted with id: {}", id);
    }

    private DeliveryDTO convertToDTO(Delivery delivery) {
        DeliveryDTO dto = new DeliveryDTO();
        dto.setId(delivery.getId());
        dto.setOrderId(delivery.getOrderId());
        dto.setDeliveryAddress(delivery.getDeliveryAddress());
        dto.setStatus(delivery.getStatus());
        dto.setEstimatedDelivery(delivery.getEstimatedDelivery());
        dto.setActualDelivery(delivery.getActualDelivery());
        dto.setPharmacyId(delivery.getPharmacy().getId());
        return dto;
    }
}

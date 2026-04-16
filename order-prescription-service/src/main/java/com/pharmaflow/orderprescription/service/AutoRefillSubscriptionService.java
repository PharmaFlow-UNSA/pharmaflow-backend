package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.AutoRefillSubscriptionDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.AutoRefillSubscriptionRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AutoRefillSubscriptionService {

    private final AutoRefillSubscriptionRepository autoRefillSubscriptionRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ModelMapper modelMapper;

    public AutoRefillSubscriptionService(AutoRefillSubscriptionRepository autoRefillSubscriptionRepository,
                                         PrescriptionRepository prescriptionRepository,
                                         ModelMapper modelMapper) {
        this.autoRefillSubscriptionRepository = autoRefillSubscriptionRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<AutoRefillSubscriptionDTO> getAllSubscriptions() {
        return autoRefillSubscriptionRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
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
        AutoRefillSubscription subscription = new AutoRefillSubscription();
        subscription.setUserId(dto.getUserId());
        subscription.setProductId(dto.getProductId());
        subscription.setDosagePerDay(dto.getDosagePerDay());
        subscription.setTabletsPerPackage(dto.getTabletsPerPackage());

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

        AutoRefillSubscription saved = autoRefillSubscriptionRepository.save(subscription);
        return convertToDTO(saved);
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
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteSubscription(Long id) {
        if (!autoRefillSubscriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subscription not found with id: " + id);
        }
        autoRefillSubscriptionRepository.deleteById(id);
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

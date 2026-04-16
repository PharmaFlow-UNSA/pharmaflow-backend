package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final ModelMapper modelMapper;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, ModelMapper modelMapper) {
        this.prescriptionRepository = prescriptionRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> getAllPrescriptions() {
        return prescriptionRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
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
        return convertToDTO(saved);
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
        return convertToDTO(saved);
    }

    @Transactional
    public void deletePrescription(Long id) {
        if (!prescriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prescription not found with id: " + id);
        }
        prescriptionRepository.deleteById(id);
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

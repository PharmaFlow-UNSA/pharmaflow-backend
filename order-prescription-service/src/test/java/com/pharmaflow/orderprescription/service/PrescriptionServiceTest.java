package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private PrescriptionService prescriptionService;

    private Prescription testPrescription;
    private PrescriptionCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        prescriptionService = new PrescriptionService(prescriptionRepository, modelMapper, objectMapper, validator);

        testPrescription = new Prescription();
        testPrescription.setId(1L);
        testPrescription.setUserId(10L);
        testPrescription.setImageUrl("https://example.com/recept.png");
        testPrescription.setStatus("PENDING");
        testPrescription.setUploadedAt(LocalDateTime.now());
        testPrescription.setOrders(new ArrayList<>());
        testPrescription.setAutoRefillSubscriptions(new ArrayList<>());

        testCreateDTO = new PrescriptionCreateDTO();
        testCreateDTO.setUserId(10L);
        testCreateDTO.setImageUrl("https://example.com/recept.png");
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Prescription> page = new PageImpl<>(List.of(testPrescription));
        when(prescriptionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PrescriptionDTO> result = prescriptionService.findAll(null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPrescriptionById_WhenExists_ShouldReturn() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));

        PrescriptionDTO result = prescriptionService.getPrescriptionById(1L);

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void getPrescriptionById_WhenNotExists_ShouldThrow() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> prescriptionService.getPrescriptionById(99L));
    }

    @Test
    void getPrescriptionsByUserId_ShouldReturnList() {
        when(prescriptionRepository.findByUserId(10L)).thenReturn(List.of(testPrescription));

        assertEquals(1, prescriptionService.getPrescriptionsByUserId(10L).size());
    }

    @Test
    void getPrescriptionsByStatus_ShouldReturnList() {
        when(prescriptionRepository.findByStatus("PENDING")).thenReturn(List.of(testPrescription));

        assertEquals(1, prescriptionService.getPrescriptionsByStatus("PENDING").size());
    }

    @Test
    void createPrescription_ShouldSucceed() {
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        PrescriptionDTO result = prescriptionService.createPrescription(testCreateDTO);

        assertNotNull(result);
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    void createPrescriptionsBatch_ShouldSaveAll() {
        when(prescriptionRepository.saveAll(any())).thenReturn(List.of(testPrescription));

        List<PrescriptionDTO> result = prescriptionService.createPrescriptionsBatch(List.of(testCreateDTO));

        assertEquals(1, result.size());
    }

    @Test
    void updatePrescription_WhenExists_ShouldUpdate() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setUserId(10L);
        dto.setImageUrl("https://example.com/recept2.png");
        dto.setStatus("APPROVED");
        PrescriptionDTO result = prescriptionService.updatePrescription(1L, dto);

        assertNotNull(result);
    }

    @Test
    void updatePrescription_WhenNotExists_ShouldThrow() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> prescriptionService.updatePrescription(99L, new PrescriptionDTO()));
    }

    @Test
    void patchPrescription_WithValidPatch_ShouldUpdate() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        PrescriptionDTO result = prescriptionService.patchPrescription(1L,
                "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"APPROVED\"}]");

        assertNotNull(result);
    }

    @Test
    void patchPrescription_WithMalformedPatch_ShouldThrow() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));

        assertThrows(PatchOperationException.class,
                () -> prescriptionService.patchPrescription(1L, "junk"));
    }

    @Test
    void deletePrescription_WhenExists_ShouldDelete() {
        when(prescriptionRepository.existsById(1L)).thenReturn(true);

        prescriptionService.deletePrescription(1L);

        verify(prescriptionRepository).deleteById(1L);
    }

    @Test
    void deletePrescription_WhenNotExists_ShouldThrow() {
        when(prescriptionRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> prescriptionService.deletePrescription(99L));
    }
}

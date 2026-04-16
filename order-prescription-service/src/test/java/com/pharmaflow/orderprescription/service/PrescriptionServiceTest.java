package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private Prescription testPrescription;

    @BeforeEach
    void setUp() {
        testPrescription = new Prescription();
        testPrescription.setId(1L);
        testPrescription.setUserId(1L);
        testPrescription.setImageUrl("/uploads/recept_1.pdf");
        testPrescription.setStatus("PENDING");
        testPrescription.setUploadedAt(LocalDateTime.now());
        testPrescription.setOrders(new ArrayList<>());
        testPrescription.setAutoRefillSubscriptions(new ArrayList<>());
    }

    @Test
    void getAllPrescriptions_WhenPrescriptionsExist_ShouldReturnPrescriptionList() {
        when(prescriptionRepository.findAll()).thenReturn(List.of(testPrescription));

        List<PrescriptionDTO> result = prescriptionService.getAllPrescriptions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPrescription.getId(), result.get(0).getId());
        assertEquals(testPrescription.getUserId(), result.get(0).getUserId());
        verify(prescriptionRepository, times(1)).findAll();
    }

    @Test
    void getPrescriptionById_WhenPrescriptionExists_ShouldReturnPrescription() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));

        PrescriptionDTO result = prescriptionService.getPrescriptionById(1L);

        assertNotNull(result);
        assertEquals(testPrescription.getId(), result.getId());
        assertEquals(testPrescription.getUserId(), result.getUserId());
        assertEquals(testPrescription.getImageUrl(), result.getImageUrl());
        assertEquals(testPrescription.getStatus(), result.getStatus());
        verify(prescriptionRepository, times(1)).findById(1L);
    }

    @Test
    void getPrescriptionById_WhenPrescriptionNotExists_ShouldThrowResourceNotFoundException() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> prescriptionService.getPrescriptionById(99L));
        verify(prescriptionRepository, times(1)).findById(99L);
    }

    @Test
    void getPrescriptionsByUserId_WhenPrescriptionsExist_ShouldReturnPrescriptionList() {
        when(prescriptionRepository.findByUserId(1L)).thenReturn(List.of(testPrescription));

        List<PrescriptionDTO> result = prescriptionService.getPrescriptionsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPrescription.getUserId(), result.get(0).getUserId());
        verify(prescriptionRepository, times(1)).findByUserId(1L);
    }

    @Test
    void createPrescription_WhenValidInput_ShouldSetStatusToPendingAndReturnPrescription() {
        PrescriptionCreateDTO createDTO = new PrescriptionCreateDTO();
        createDTO.setUserId(1L);
        createDTO.setImageUrl("/uploads/recept_1.pdf");

        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PrescriptionDTO result = prescriptionService.createPrescription(createDTO);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(createDTO.getUserId(), result.getUserId());
        assertEquals(createDTO.getImageUrl(), result.getImageUrl());
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));
    }

    @Test
    void updatePrescription_WhenPrescriptionExists_ShouldReturnUpdatedPrescription() {
        PrescriptionDTO updateDTO = new PrescriptionDTO();
        updateDTO.setUserId(1L);
        updateDTO.setImageUrl("/uploads/recept_updated.pdf");
        updateDTO.setStatus("APPROVED");
        updateDTO.setReviewerNotes("Approved by pharmacist");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionDTO result = prescriptionService.updatePrescription(1L, updateDTO);

        assertNotNull(result);
        assertEquals(updateDTO.getStatus(), result.getStatus());
        assertEquals(updateDTO.getImageUrl(), result.getImageUrl());
        verify(prescriptionRepository, times(1)).findById(1L);
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));
    }

    @Test
    void updatePrescription_WhenPrescriptionNotExists_ShouldThrowResourceNotFoundException() {
        PrescriptionDTO updateDTO = new PrescriptionDTO();

        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> prescriptionService.updatePrescription(99L, updateDTO));
        verify(prescriptionRepository, times(1)).findById(99L);
        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    @Test
    void deletePrescription_WhenPrescriptionExists_ShouldDeleteSuccessfully() {
        when(prescriptionRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> prescriptionService.deletePrescription(1L));
        verify(prescriptionRepository, times(1)).existsById(1L);
        verify(prescriptionRepository, times(1)).deleteById(1L);
    }

    @Test
    void deletePrescription_WhenPrescriptionNotExists_ShouldThrowResourceNotFoundException() {
        when(prescriptionRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> prescriptionService.deletePrescription(99L));
        verify(prescriptionRepository, times(1)).existsById(99L);
        verify(prescriptionRepository, never()).deleteById(any());
    }
}

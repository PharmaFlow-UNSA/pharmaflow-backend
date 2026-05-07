package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.AutoRefillSubscriptionDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.AutoRefillSubscriptionRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoRefillSubscriptionServiceTest {

    @Mock private AutoRefillSubscriptionRepository autoRefillSubscriptionRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AutoRefillSubscriptionService autoRefillSubscriptionService;

    private AutoRefillSubscription testSubscription;
    private AutoRefillSubscriptionDTO testDTO;

    @BeforeEach
    void setUp() {
        autoRefillSubscriptionService = new AutoRefillSubscriptionService(
                autoRefillSubscriptionRepository, prescriptionRepository, modelMapper, objectMapper, validator);

        testSubscription = new AutoRefillSubscription();
        testSubscription.setId(1L);
        testSubscription.setUserId(10L);
        testSubscription.setProductId(100L);
        testSubscription.setDosagePerDay(2);
        testSubscription.setTabletsPerPackage(60);
        testSubscription.setIntervalDays(30);
        testSubscription.setNextOrderDate(LocalDate.now().plusDays(30));
        testSubscription.setStatus("ACTIVE");
        testSubscription.setShippingAddress("Marsala Tita 10");

        testDTO = new AutoRefillSubscriptionDTO();
        testDTO.setUserId(10L);
        testDTO.setProductId(100L);
        testDTO.setDosagePerDay(2);
        testDTO.setTabletsPerPackage(60);
        testDTO.setStatus("ACTIVE");
        testDTO.setShippingAddress("Marsala Tita 10");
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AutoRefillSubscription> page = new PageImpl<>(List.of(testSubscription));
        when(autoRefillSubscriptionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AutoRefillSubscriptionDTO> result = autoRefillSubscriptionService.findAll(null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getSubscriptionById_WhenExists_ShouldReturn() {
        when(autoRefillSubscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));

        AutoRefillSubscriptionDTO result = autoRefillSubscriptionService.getSubscriptionById(1L);

        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void getSubscriptionById_WhenNotExists_ShouldThrow() {
        when(autoRefillSubscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> autoRefillSubscriptionService.getSubscriptionById(99L));
    }

    @Test
    void getSubscriptionsByUserId_ShouldReturnList() {
        when(autoRefillSubscriptionRepository.findByUserId(10L)).thenReturn(List.of(testSubscription));

        assertEquals(1, autoRefillSubscriptionService.getSubscriptionsByUserId(10L).size());
    }

    @Test
    void getSubscriptionsByStatus_ShouldReturnList() {
        when(autoRefillSubscriptionRepository.findByStatus("ACTIVE")).thenReturn(List.of(testSubscription));

        assertEquals(1, autoRefillSubscriptionService.getSubscriptionsByStatus("ACTIVE").size());
    }

    @Test
    void createSubscription_ShouldSucceed() {
        when(autoRefillSubscriptionRepository.save(any(AutoRefillSubscription.class))).thenReturn(testSubscription);

        AutoRefillSubscriptionDTO result = autoRefillSubscriptionService.createSubscription(testDTO);

        assertNotNull(result);
        verify(autoRefillSubscriptionRepository).save(any(AutoRefillSubscription.class));
    }

    @Test
    void createSubscriptionsBatch_ShouldSaveAll() {
        when(autoRefillSubscriptionRepository.saveAll(any())).thenReturn(List.of(testSubscription));

        List<AutoRefillSubscriptionDTO> result = autoRefillSubscriptionService.createSubscriptionsBatch(List.of(testDTO));

        assertEquals(1, result.size());
    }

    @Test
    void updateSubscription_WhenExists_ShouldUpdate() {
        when(autoRefillSubscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
        when(autoRefillSubscriptionRepository.save(any(AutoRefillSubscription.class))).thenReturn(testSubscription);

        AutoRefillSubscriptionDTO result = autoRefillSubscriptionService.updateSubscription(1L, testDTO);

        assertNotNull(result);
    }

    @Test
    void updateSubscription_WhenNotExists_ShouldThrow() {
        when(autoRefillSubscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> autoRefillSubscriptionService.updateSubscription(99L, testDTO));
    }

    @Test
    void patchSubscription_WithValidPatch_ShouldUpdate() {
        when(autoRefillSubscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));
        when(autoRefillSubscriptionRepository.save(any(AutoRefillSubscription.class))).thenReturn(testSubscription);

        AutoRefillSubscriptionDTO result = autoRefillSubscriptionService.patchSubscription(1L,
                "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"PAUSED\"}]");

        assertNotNull(result);
    }

    @Test
    void patchSubscription_WithMalformedPatch_ShouldThrow() {
        when(autoRefillSubscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));

        assertThrows(PatchOperationException.class,
                () -> autoRefillSubscriptionService.patchSubscription(1L, "junk"));
    }

    @Test
    void deleteSubscription_WhenExists_ShouldDelete() {
        when(autoRefillSubscriptionRepository.existsById(1L)).thenReturn(true);

        autoRefillSubscriptionService.deleteSubscription(1L);

        verify(autoRefillSubscriptionRepository).deleteById(1L);
    }

    @Test
    void deleteSubscription_WhenNotExists_ShouldThrow() {
        when(autoRefillSubscriptionRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> autoRefillSubscriptionService.deleteSubscription(99L));
    }
}

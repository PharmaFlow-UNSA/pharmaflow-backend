package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.AutoRefillSubscriptionDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import com.pharmaflow.orderprescription.repositories.AutoRefillSubscriptionRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoRefillSubscriptionServiceTest {

    @Mock
    private AutoRefillSubscriptionRepository autoRefillSubscriptionRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AutoRefillSubscriptionService autoRefillSubscriptionService;

    private AutoRefillSubscription testSubscription;

    @BeforeEach
    void setUp() {
        testSubscription = new AutoRefillSubscription();
        testSubscription.setId(1L);
        testSubscription.setUserId(1L);
        testSubscription.setProductId(100L);
        testSubscription.setDosagePerDay(1);
        testSubscription.setTabletsPerPackage(30);
        testSubscription.setIntervalDays(30);
        testSubscription.setNextOrderDate(LocalDate.now().plusDays(30));
        testSubscription.setStatus("ACTIVE");
        testSubscription.setShippingAddress("Ulica 1, Sarajevo");
        testSubscription.setPrescription(null);
    }

    @Test
    void getAllSubscriptions_WhenSubscriptionsExist_ShouldReturnSubscriptionList() {
        when(autoRefillSubscriptionRepository.findAll()).thenReturn(List.of(testSubscription));

        List<AutoRefillSubscriptionDTO> result = autoRefillSubscriptionService.getAllSubscriptions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSubscription.getId(), result.get(0).getId());
        assertEquals(testSubscription.getUserId(), result.get(0).getUserId());
        verify(autoRefillSubscriptionRepository, times(1)).findAll();
    }

    @Test
    void getSubscriptionById_WhenSubscriptionExists_ShouldReturnSubscription() {
        when(autoRefillSubscriptionRepository.findById(1L)).thenReturn(Optional.of(testSubscription));

        AutoRefillSubscriptionDTO result = autoRefillSubscriptionService.getSubscriptionById(1L);

        assertNotNull(result);
        assertEquals(testSubscription.getId(), result.getId());
        assertEquals(testSubscription.getUserId(), result.getUserId());
        assertEquals(testSubscription.getProductId(), result.getProductId());
        assertEquals(testSubscription.getIntervalDays(), result.getIntervalDays());
        assertEquals(testSubscription.getStatus(), result.getStatus());
        assertEquals(testSubscription.getShippingAddress(), result.getShippingAddress());
        verify(autoRefillSubscriptionRepository, times(1)).findById(1L);
    }

    @Test
    void getSubscriptionById_WhenSubscriptionNotExists_ShouldThrowResourceNotFoundException() {
        when(autoRefillSubscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> autoRefillSubscriptionService.getSubscriptionById(99L));
        verify(autoRefillSubscriptionRepository, times(1)).findById(99L);
    }

    @Test
    void getSubscriptionsByUserId_WhenSubscriptionsExist_ShouldReturnSubscriptionList() {
        when(autoRefillSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(testSubscription));

        List<AutoRefillSubscriptionDTO> result = autoRefillSubscriptionService.getSubscriptionsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSubscription.getUserId(), result.get(0).getUserId());
        verify(autoRefillSubscriptionRepository, times(1)).findByUserId(1L);
    }

    @Test
    void createSubscription_WhenValidInput_ShouldCalculateIntervalDaysAndReturnSubscription() {
        AutoRefillSubscriptionDTO createDTO = new AutoRefillSubscriptionDTO();
        createDTO.setUserId(1L);
        createDTO.setProductId(100L);
        createDTO.setDosagePerDay(1);
        createDTO.setTabletsPerPackage(30);
        createDTO.setStatus("ACTIVE");
        createDTO.setShippingAddress("Ulica 1, Sarajevo");
        createDTO.setPrescriptionId(null);

        when(autoRefillSubscriptionRepository.save(any(AutoRefillSubscription.class))).thenAnswer(invocation -> {
            AutoRefillSubscription saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        AutoRefillSubscriptionDTO result = autoRefillSubscriptionService.createSubscription(createDTO);

        assertNotNull(result);
        assertEquals(30, result.getIntervalDays());
        assertEquals(LocalDate.now().plusDays(30), result.getNextOrderDate());
        assertEquals(createDTO.getUserId(), result.getUserId());
        assertEquals(createDTO.getProductId(), result.getProductId());
        assertEquals(createDTO.getStatus(), result.getStatus());
        verify(autoRefillSubscriptionRepository, times(1)).save(any(AutoRefillSubscription.class));
    }

    @Test
    void deleteSubscription_WhenSubscriptionExists_ShouldDeleteSuccessfully() {
        when(autoRefillSubscriptionRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> autoRefillSubscriptionService.deleteSubscription(1L));
        verify(autoRefillSubscriptionRepository, times(1)).existsById(1L);
        verify(autoRefillSubscriptionRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteSubscription_WhenSubscriptionNotExists_ShouldThrowResourceNotFoundException() {
        when(autoRefillSubscriptionRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> autoRefillSubscriptionService.deleteSubscription(99L));
        verify(autoRefillSubscriptionRepository, times(1)).existsById(99L);
        verify(autoRefillSubscriptionRepository, never()).deleteById(any());
    }
}

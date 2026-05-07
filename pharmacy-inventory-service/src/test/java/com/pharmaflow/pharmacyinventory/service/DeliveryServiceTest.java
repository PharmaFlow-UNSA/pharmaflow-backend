package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.pharmacyinventory.dto.DeliveryDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Delivery;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.DeliveryRepository;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private DeliveryService deliveryService;

    private Pharmacy testPharmacy;
    private Delivery testDelivery;
    private DeliveryDTO testDeliveryDTO;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(deliveryRepository, pharmacyRepository, modelMapper, objectMapper, validator);

        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);

        testDelivery = new Delivery();
        testDelivery.setId(1L);
        testDelivery.setOrderId(100L);
        testDelivery.setDeliveryAddress("Marsala Tita 10");
        testDelivery.setStatus("PREPARING");
        testDelivery.setEstimatedDelivery(LocalDateTime.of(2026, 5, 6, 12, 0));
        testDelivery.setPharmacy(testPharmacy);

        testDeliveryDTO = new DeliveryDTO();
        testDeliveryDTO.setOrderId(100L);
        testDeliveryDTO.setDeliveryAddress("Marsala Tita 10");
        testDeliveryDTO.setStatus("PREPARING");
        testDeliveryDTO.setEstimatedDelivery(LocalDateTime.of(2026, 5, 6, 12, 0));
        testDeliveryDTO.setPharmacyId(1L);
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Delivery> page = new PageImpl<>(List.of(testDelivery));
        when(deliveryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<DeliveryDTO> result = deliveryService.findAll(null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getDeliveryById_WhenExists_ShouldReturn() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));

        DeliveryDTO result = deliveryService.getDeliveryById(1L);

        assertEquals("PREPARING", result.getStatus());
    }

    @Test
    void getDeliveryById_WhenNotExists_ShouldThrow() {
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.getDeliveryById(99L));
    }

    @Test
    void getDeliveriesByPharmacyId_ShouldReturnList() {
        when(deliveryRepository.findByPharmacyId(1L)).thenReturn(List.of(testDelivery));

        assertEquals(1, deliveryService.getDeliveriesByPharmacyId(1L).size());
    }

    @Test
    void getDeliveriesByOrderId_ShouldReturnList() {
        when(deliveryRepository.findByOrderId(100L)).thenReturn(List.of(testDelivery));

        assertEquals(1, deliveryService.getDeliveriesByOrderId(100L).size());
    }

    @Test
    void getDeliveriesByStatus_ShouldReturnList() {
        when(deliveryRepository.findByStatus("PREPARING")).thenReturn(List.of(testDelivery));

        assertEquals(1, deliveryService.getDeliveriesByStatus("PREPARING").size());
    }

    @Test
    void createDelivery_WhenPharmacyExists_ShouldSucceed() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);

        DeliveryDTO result = deliveryService.createDelivery(testDeliveryDTO);

        assertEquals("PREPARING", result.getStatus());
    }

    @Test
    void createDelivery_WhenPharmacyNotExists_ShouldThrow() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.createDelivery(testDeliveryDTO));
    }

    @Test
    void createDeliveriesBatch_ShouldSaveAll() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(deliveryRepository.saveAll(any())).thenReturn(List.of(testDelivery));

        List<DeliveryDTO> result = deliveryService.createDeliveriesBatch(List.of(testDeliveryDTO));

        assertEquals(1, result.size());
    }

    @Test
    void updateDelivery_WhenExists_ShouldUpdate() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);

        DeliveryDTO result = deliveryService.updateDelivery(1L, testDeliveryDTO);

        assertNotNull(result);
    }

    @Test
    void updateDelivery_WhenNotExists_ShouldThrow() {
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.updateDelivery(99L, testDeliveryDTO));
    }

    @Test
    void patchDelivery_WithValidPatch_ShouldUpdate() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);

        DeliveryDTO result = deliveryService.patchDelivery(1L,
                "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"DELIVERED\"}]");

        assertNotNull(result);
    }

    @Test
    void patchDelivery_WithMalformedPatch_ShouldThrow() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));

        assertThrows(PatchOperationException.class,
                () -> deliveryService.patchDelivery(1L, "junk"));
    }

    @Test
    void deleteDelivery_WhenExists_ShouldDelete() {
        when(deliveryRepository.existsById(1L)).thenReturn(true);

        deliveryService.deleteDelivery(1L);

        verify(deliveryRepository).deleteById(1L);
    }

    @Test
    void deleteDelivery_WhenNotExists_ShouldThrow() {
        when(deliveryRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.deleteDelivery(99L));
    }
}

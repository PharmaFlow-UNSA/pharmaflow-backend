package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.DeliveryDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Delivery;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.DeliveryRepository;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DeliveryService deliveryService;

    private Delivery testDelivery;
    private DeliveryDTO testDeliveryDTO;
    private Pharmacy testPharmacy;

    @BeforeEach
    void setUp() {
        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);
        testPharmacy.setName("Test Pharmacy");

        LocalDateTime now = LocalDateTime.now();

        testDelivery = new Delivery();
        testDelivery.setId(1L);
        testDelivery.setOrderId(1L);
        testDelivery.setDeliveryAddress("Ulica 1, Sarajevo");
        testDelivery.setStatus("PREPARING");
        testDelivery.setEstimatedDelivery(now.plusDays(2));
        testDelivery.setActualDelivery(null);
        testDelivery.setPharmacy(testPharmacy);

        testDeliveryDTO = new DeliveryDTO();
        testDeliveryDTO.setId(1L);
        testDeliveryDTO.setOrderId(1L);
        testDeliveryDTO.setDeliveryAddress("Ulica 1, Sarajevo");
        testDeliveryDTO.setStatus("PREPARING");
        testDeliveryDTO.setEstimatedDelivery(now.plusDays(2));
        testDeliveryDTO.setActualDelivery(null);
        testDeliveryDTO.setPharmacyId(1L);
    }

    @Test
    void getAllDeliveries_ShouldReturnListOfDeliveries() {
        // Arrange
        List<Delivery> deliveries = List.of(testDelivery);
        when(deliveryRepository.findAll()).thenReturn(deliveries);

        // Act
        List<DeliveryDTO> result = deliveryService.getAllDeliveries();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getOrderId());
        assertEquals("PREPARING", result.get(0).getStatus());
        verify(deliveryRepository, times(1)).findAll();
    }

    @Test
    void getDeliveryById_WhenDeliveryExists_ShouldReturnDelivery() {
        // Arrange
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));

        // Act
        DeliveryDTO result = deliveryService.getDeliveryById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getOrderId());
        assertEquals("Ulica 1, Sarajevo", result.getDeliveryAddress());
        assertEquals("PREPARING", result.getStatus());
        assertNull(result.getActualDelivery());
        assertEquals(1L, result.getPharmacyId());
        verify(deliveryRepository, times(1)).findById(1L);
    }

    @Test
    void getDeliveryById_WhenDeliveryNotExists_ShouldThrowException() {
        // Arrange
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.getDeliveryById(99L));
        assertEquals("Delivery not found with id: 99", exception.getMessage());
        verify(deliveryRepository, times(1)).findById(99L);
    }

    @Test
    void getDeliveriesByOrderId_ShouldReturnListOfDeliveries() {
        // Arrange
        List<Delivery> deliveries = List.of(testDelivery);
        when(deliveryRepository.findByOrderId(1L)).thenReturn(deliveries);

        // Act
        List<DeliveryDTO> result = deliveryService.getDeliveriesByOrderId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getOrderId());
        assertEquals("Ulica 1, Sarajevo", result.get(0).getDeliveryAddress());
        verify(deliveryRepository, times(1)).findByOrderId(1L);
    }

    @Test
    void createDelivery_WhenPharmacyExists_ShouldReturnCreatedDelivery() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);

        // Act
        DeliveryDTO result = deliveryService.createDelivery(testDeliveryDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals("Ulica 1, Sarajevo", result.getDeliveryAddress());
        assertEquals("PREPARING", result.getStatus());
        assertEquals(1L, result.getPharmacyId());
        verify(pharmacyRepository, times(1)).findById(1L);
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    void createDelivery_WhenPharmacyNotExists_ShouldThrowException() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.createDelivery(testDeliveryDTO));
        assertEquals("Pharmacy not found with id: 1", exception.getMessage());
        verify(pharmacyRepository, times(1)).findById(1L);
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void deleteDelivery_WhenDeliveryExists_ShouldDeleteDelivery() {
        // Arrange
        when(deliveryRepository.existsById(1L)).thenReturn(true);

        // Act
        deliveryService.deleteDelivery(1L);

        // Assert
        verify(deliveryRepository, times(1)).existsById(1L);
        verify(deliveryRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteDelivery_WhenDeliveryNotExists_ShouldThrowException() {
        // Arrange
        when(deliveryRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.deleteDelivery(99L));
        assertEquals("Delivery not found with id: 99", exception.getMessage());
        verify(deliveryRepository, times(1)).existsById(99L);
        verify(deliveryRepository, never()).deleteById(anyLong());
    }
}

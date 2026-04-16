package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.InventoryDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Inventory;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.InventoryRepository;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
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
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory testInventory;
    private InventoryDTO testInventoryDTO;
    private Pharmacy testPharmacy;

    @BeforeEach
    void setUp() {
        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);
        testPharmacy.setName("Test Pharmacy");

        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setProductId(100L);
        testInventory.setQuantity(50);
        testInventory.setReorderLevel(10);
        testInventory.setLastRestocked(LocalDate.now());
        testInventory.setPharmacy(testPharmacy);

        testInventoryDTO = new InventoryDTO();
        testInventoryDTO.setId(1L);
        testInventoryDTO.setProductId(100L);
        testInventoryDTO.setQuantity(50);
        testInventoryDTO.setReorderLevel(10);
        testInventoryDTO.setLastRestocked(LocalDate.now());
        testInventoryDTO.setPharmacyId(1L);
    }

    @Test
    void getAllInventoryItems_ShouldReturnListOfItems() {
        // Arrange
        List<Inventory> inventories = List.of(testInventory);
        when(inventoryRepository.findAll()).thenReturn(inventories);

        // Act
        List<InventoryDTO> result = inventoryService.getAllInventoryItems();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getProductId());
        assertEquals(50, result.get(0).getQuantity());
        verify(inventoryRepository, times(1)).findAll();
    }

    @Test
    void getInventoryById_WhenInventoryExists_ShouldReturnInventory() {
        // Arrange
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));

        // Act
        InventoryDTO result = inventoryService.getInventoryById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getProductId());
        assertEquals(50, result.getQuantity());
        assertEquals(10, result.getReorderLevel());
        assertEquals(1L, result.getPharmacyId());
        verify(inventoryRepository, times(1)).findById(1L);
    }

    @Test
    void getInventoryById_WhenInventoryNotExists_ShouldThrowException() {
        // Arrange
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getInventoryById(99L));
        assertEquals("Inventory not found with id: 99", exception.getMessage());
        verify(inventoryRepository, times(1)).findById(99L);
    }

    @Test
    void getInventoryByPharmacyId_ShouldReturnListOfItems() {
        // Arrange
        List<Inventory> inventories = List.of(testInventory);
        when(inventoryRepository.findByPharmacyId(1L)).thenReturn(inventories);

        // Act
        List<InventoryDTO> result = inventoryService.getInventoryByPharmacyId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getProductId());
        assertEquals(1L, result.get(0).getPharmacyId());
        verify(inventoryRepository, times(1)).findByPharmacyId(1L);
    }

    @Test
    void createInventory_WhenPharmacyExists_ShouldReturnCreatedInventory() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // Act
        InventoryDTO result = inventoryService.createInventory(testInventoryDTO);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getProductId());
        assertEquals(50, result.getQuantity());
        assertEquals(1L, result.getPharmacyId());
        verify(pharmacyRepository, times(1)).findById(1L);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void createInventory_WhenPharmacyNotExists_ShouldThrowException() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.createInventory(testInventoryDTO));
        assertEquals("Pharmacy not found with id: 1", exception.getMessage());
        verify(pharmacyRepository, times(1)).findById(1L);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void deleteInventory_WhenInventoryExists_ShouldDeleteInventory() {
        // Arrange
        when(inventoryRepository.existsById(1L)).thenReturn(true);

        // Act
        inventoryService.deleteInventory(1L);

        // Assert
        verify(inventoryRepository, times(1)).existsById(1L);
        verify(inventoryRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteInventory_WhenInventoryNotExists_ShouldThrowException() {
        // Arrange
        when(inventoryRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.deleteInventory(99L));
        assertEquals("Inventory not found with id: 99", exception.getMessage());
        verify(inventoryRepository, times(1)).existsById(99L);
        verify(inventoryRepository, never()).deleteById(anyLong());
    }
}

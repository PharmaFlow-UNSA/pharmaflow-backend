package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.InventoryDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Inventory;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.InventoryRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private InventoryService inventoryService;

    private Pharmacy testPharmacy;
    private Inventory testInventory;
    private InventoryDTO testInventoryDTO;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, pharmacyRepository, modelMapper, objectMapper, validator);

        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);
        testPharmacy.setName("Apoteka");

        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setProductId(100L);
        testInventory.setQuantity(50);
        testInventory.setReorderLevel(10);
        testInventory.setLastRestocked(LocalDate.of(2026, 5, 1));
        testInventory.setPharmacy(testPharmacy);

        testInventoryDTO = new InventoryDTO();
        testInventoryDTO.setProductId(100L);
        testInventoryDTO.setQuantity(50);
        testInventoryDTO.setReorderLevel(10);
        testInventoryDTO.setLastRestocked(LocalDate.of(2026, 5, 1));
        testInventoryDTO.setPharmacyId(1L);
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = new PageImpl<>(List.of(testInventory));
        when(inventoryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<InventoryDTO> result = inventoryService.findAll(null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getProductId());
    }

    @Test
    void getInventoryById_WhenExists_ShouldReturn() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));

        InventoryDTO result = inventoryService.getInventoryById(1L);

        assertEquals(50, result.getQuantity());
    }

    @Test
    void getInventoryById_WhenNotExists_ShouldThrow() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getInventoryById(99L));
    }

    @Test
    void getInventoryByPharmacyId_ShouldReturnList() {
        when(inventoryRepository.findByPharmacyId(1L)).thenReturn(List.of(testInventory));

        List<InventoryDTO> result = inventoryService.getInventoryByPharmacyId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getInventoryByProductId_ShouldReturnList() {
        when(inventoryRepository.findByProductId(100L)).thenReturn(List.of(testInventory));

        List<InventoryDTO> result = inventoryService.getInventoryByProductId(100L);

        assertEquals(1, result.size());
    }

    @Test
    void createInventory_WhenPharmacyExists_ShouldSucceed() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryDTO result = inventoryService.createInventory(testInventoryDTO);

        assertEquals(50, result.getQuantity());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void createInventory_WhenPharmacyNotExists_ShouldThrow() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.createInventory(testInventoryDTO));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void createInventoriesBatch_ShouldSaveAll() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(inventoryRepository.saveAll(any())).thenReturn(List.of(testInventory));

        List<InventoryDTO> result = inventoryService.createInventoriesBatch(List.of(testInventoryDTO));

        assertEquals(1, result.size());
        verify(inventoryRepository).saveAll(any());
    }

    @Test
    void updateInventory_WhenExists_ShouldUpdate() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryDTO result = inventoryService.updateInventory(1L, testInventoryDTO);

        assertEquals(50, result.getQuantity());
    }

    @Test
    void updateInventory_WhenNotExists_ShouldThrow() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.updateInventory(99L, testInventoryDTO));
    }

    @Test
    void patchInventory_WithValidPatch_ShouldUpdate() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        String patch = "[{\"op\":\"replace\",\"path\":\"/quantity\",\"value\":100}]";
        InventoryDTO result = inventoryService.patchInventory(1L, patch);

        assertNotNull(result);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void patchInventory_WithMalformedPatch_ShouldThrow() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));

        assertThrows(PatchOperationException.class,
                () -> inventoryService.patchInventory(1L, "garbage"));
    }

    @Test
    void deleteInventory_WhenExists_ShouldDelete() {
        when(inventoryRepository.existsById(1L)).thenReturn(true);

        inventoryService.deleteInventory(1L);

        verify(inventoryRepository).deleteById(1L);
    }

    @Test
    void deleteInventory_WhenNotExists_ShouldThrow() {
        when(inventoryRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.deleteInventory(99L));
    }
}

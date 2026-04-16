package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.InventoryDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Inventory;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.InventoryRepository;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;

    public InventoryService(InventoryRepository inventoryRepository,
                            PharmacyRepository pharmacyRepository,
                            ModelMapper modelMapper) {
        this.inventoryRepository = inventoryRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> getAllInventoryItems() {
        return inventoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryDTO getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
        return convertToDTO(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> getInventoryByPharmacyId(Long pharmacyId) {
        return inventoryRepository.findByPharmacyId(pharmacyId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public InventoryDTO createInventory(InventoryDTO inventoryDTO) {
        Pharmacy pharmacy = pharmacyRepository.findById(inventoryDTO.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found with id: " + inventoryDTO.getPharmacyId()));

        Inventory inventory = new Inventory();
        inventory.setProductId(inventoryDTO.getProductId());
        inventory.setQuantity(inventoryDTO.getQuantity());
        inventory.setReorderLevel(inventoryDTO.getReorderLevel());
        inventory.setLastRestocked(inventoryDTO.getLastRestocked());
        inventory.setPharmacy(pharmacy);

        Inventory saved = inventoryRepository.save(inventory);
        return convertToDTO(saved);
    }

    @Transactional
    public InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        inventory.setProductId(inventoryDTO.getProductId());
        inventory.setQuantity(inventoryDTO.getQuantity());
        inventory.setReorderLevel(inventoryDTO.getReorderLevel());
        inventory.setLastRestocked(inventoryDTO.getLastRestocked());

        Inventory updated = inventoryRepository.save(inventory);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteInventory(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory not found with id: " + id);
        }
        inventoryRepository.deleteById(id);
    }

    private InventoryDTO convertToDTO(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProductId());
        dto.setQuantity(inventory.getQuantity());
        dto.setReorderLevel(inventory.getReorderLevel());
        dto.setLastRestocked(inventory.getLastRestocked());
        dto.setPharmacyId(inventory.getPharmacy().getId());
        return dto;
    }
}

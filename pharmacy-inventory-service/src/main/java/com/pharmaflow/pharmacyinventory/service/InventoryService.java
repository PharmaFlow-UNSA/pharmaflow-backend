package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.pharmacyinventory.dto.InventoryDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Inventory;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.InventoryRepository;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import com.pharmaflow.pharmacyinventory.specifications.InventorySpecs;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public InventoryService(InventoryRepository inventoryRepository,
                            PharmacyRepository pharmacyRepository,
                            ModelMapper modelMapper,
                            ObjectMapper objectMapper,
                            Validator validator) {
        this.inventoryRepository = inventoryRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<InventoryDTO> findAll(Long pharmacyId, Long productId, Integer minQuantity, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Inventory> spec = Specification
                .where(InventorySpecs.hasPharmacyId(pharmacyId))
                .and(InventorySpecs.hasProductId(productId))
                .and(InventorySpecs.quantityAtLeast(minQuantity));

        Page<InventoryDTO> result = inventoryRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("Inventory findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pharmacy not found with id: " + inventoryDTO.getPharmacyId()));

        Inventory inventory = new Inventory();
        inventory.setProductId(inventoryDTO.getProductId());
        inventory.setQuantity(inventoryDTO.getQuantity());
        inventory.setReorderLevel(inventoryDTO.getReorderLevel());
        inventory.setLastRestocked(inventoryDTO.getLastRestocked());
        inventory.setPharmacy(pharmacy);

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<InventoryDTO> createInventoriesBatch(List<InventoryDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<Inventory> inventories = new ArrayList<>();

        for (InventoryDTO dto : dtos) {
            Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pharmacy not found with id: " + dto.getPharmacyId()));

            Inventory inventory = new Inventory();
            inventory.setProductId(dto.getProductId());
            inventory.setQuantity(dto.getQuantity());
            inventory.setReorderLevel(dto.getReorderLevel());
            inventory.setLastRestocked(dto.getLastRestocked());
            inventory.setPharmacy(pharmacy);

            inventories.add(inventory);
        }

        List<Inventory> saved = inventoryRepository.saveAll(inventories);
        log.info("Batch created {} inventory items in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
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
        log.info("Inventory updated with id: {}", updated.getId());
        return convertToDTO(updated);
    }

    @Transactional
    public InventoryDTO patchInventory(Long id, String patchDocument) {
        try {
            Inventory inventory = inventoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

            // Patch the DTO (not the entity directly) to keep pharmacy relation safe
            InventoryDTO currentDTO = convertToDTO(inventory);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            InventoryDTO patchedDTO = objectMapper.treeToValue(patchedJson, InventoryDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<InventoryDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            inventory.setProductId(patchedDTO.getProductId());
            inventory.setQuantity(patchedDTO.getQuantity());
            inventory.setReorderLevel(patchedDTO.getReorderLevel());
            inventory.setLastRestocked(patchedDTO.getLastRestocked());

            // pharmacyId change → relink to a different pharmacy
            if (patchedDTO.getPharmacyId() != null
                    && !patchedDTO.getPharmacyId().equals(inventory.getPharmacy().getId())) {
                Pharmacy pharmacy = pharmacyRepository.findById(patchedDTO.getPharmacyId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Pharmacy not found with id: " + patchedDTO.getPharmacyId()));
                inventory.setPharmacy(pharmacy);
            }

            Inventory saved = inventoryRepository.save(inventory);
            log.info("Inventory patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteInventory(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory not found with id: " + id);
        }
        inventoryRepository.deleteById(id);
        log.info("Inventory deleted with id: {}", id);
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

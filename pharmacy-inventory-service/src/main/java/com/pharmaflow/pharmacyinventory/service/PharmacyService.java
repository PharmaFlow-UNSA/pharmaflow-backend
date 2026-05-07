package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.exception.DuplicateResourceException;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import com.pharmaflow.pharmacyinventory.specifications.PharmacySpecs;
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
public class PharmacyService {

    private static final Logger log = LoggerFactory.getLogger(PharmacyService.class);

    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PharmacyService(PharmacyRepository pharmacyRepository,
                           ModelMapper modelMapper,
                           ObjectMapper objectMapper,
                           Validator validator) {
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<PharmacyDTO> findAll(String name, String city, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Pharmacy> spec = Specification
                .where(PharmacySpecs.nameContains(name))
                .and(PharmacySpecs.cityEquals(city));

        Page<PharmacyDTO> result = pharmacyRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("Pharmacy findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    @Transactional(readOnly = true)
    public PharmacyDTO getPharmacyById(Long id) {
        Pharmacy pharmacy = pharmacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found with id: " + id));
        return convertToDTO(pharmacy);
    }

    @Transactional
    public PharmacyDTO createPharmacy(PharmacyCreateDTO createDTO) {
        if (pharmacyRepository.existsByName(createDTO.getName())) {
            throw new DuplicateResourceException("Pharmacy with name '" + createDTO.getName() + "' already exists");
        }

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(createDTO.getName());
        pharmacy.setAddress(createDTO.getAddress());
        pharmacy.setCity(createDTO.getCity());
        pharmacy.setPhoneNumber(createDTO.getPhoneNumber());
        pharmacy.setEmail(createDTO.getEmail());
        pharmacy.setOpeningHours(createDTO.getOpeningHours());

        Pharmacy saved = pharmacyRepository.save(pharmacy);
        log.info("Pharmacy created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<PharmacyDTO> createPharmaciesBatch(List<PharmacyCreateDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<Pharmacy> pharmacies = new ArrayList<>();

        for (PharmacyCreateDTO dto : dtos) {
            if (pharmacyRepository.existsByName(dto.getName())) {
                throw new DuplicateResourceException("Pharmacy with name '" + dto.getName() + "' already exists");
            }

            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setName(dto.getName());
            pharmacy.setAddress(dto.getAddress());
            pharmacy.setCity(dto.getCity());
            pharmacy.setPhoneNumber(dto.getPhoneNumber());
            pharmacy.setEmail(dto.getEmail());
            pharmacy.setOpeningHours(dto.getOpeningHours());

            pharmacies.add(pharmacy);
        }

        List<Pharmacy> saved = pharmacyRepository.saveAll(pharmacies);
        log.info("Batch created {} pharmacies in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public PharmacyDTO updatePharmacy(Long id, PharmacyCreateDTO createDTO) {
        Pharmacy pharmacy = pharmacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found with id: " + id));

        if (!pharmacy.getName().equals(createDTO.getName()) && pharmacyRepository.existsByName(createDTO.getName())) {
            throw new DuplicateResourceException("Pharmacy with name '" + createDTO.getName() + "' already exists");
        }

        pharmacy.setName(createDTO.getName());
        pharmacy.setAddress(createDTO.getAddress());
        pharmacy.setCity(createDTO.getCity());
        pharmacy.setPhoneNumber(createDTO.getPhoneNumber());
        pharmacy.setEmail(createDTO.getEmail());
        pharmacy.setOpeningHours(createDTO.getOpeningHours());

        Pharmacy updated = pharmacyRepository.save(pharmacy);
        log.info("Pharmacy updated with id: {}", updated.getId());
        return convertToDTO(updated);
    }

    @Transactional
    public PharmacyDTO patchPharmacy(Long id, String patchDocument) {
        try {
            Pharmacy pharmacy = pharmacyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found with id: " + id));

            // Patch the DTO (not the entity) to avoid infinite recursion through bidirectional relations
            PharmacyDTO currentDTO = convertToDTO(pharmacy);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            PharmacyDTO patchedDTO = objectMapper.treeToValue(patchedJson, PharmacyDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so that constraints
            // (@Email, @Pattern, @Size, @NotBlank, ...) apply even after a JSON Patch.
            Set<ConstraintViolation<PharmacyDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            if (patchedDTO.getName() != null && !patchedDTO.getName().equals(pharmacy.getName())) {
                if (pharmacyRepository.existsByName(patchedDTO.getName())) {
                    throw new DuplicateResourceException(
                            "Pharmacy with name '" + patchedDTO.getName() + "' already exists");
                }
            }

            pharmacy.setName(patchedDTO.getName());
            pharmacy.setAddress(patchedDTO.getAddress());
            pharmacy.setCity(patchedDTO.getCity());
            pharmacy.setPhoneNumber(patchedDTO.getPhoneNumber());
            pharmacy.setEmail(patchedDTO.getEmail());
            pharmacy.setOpeningHours(patchedDTO.getOpeningHours());

            Pharmacy saved = pharmacyRepository.save(pharmacy);
            log.info("Pharmacy patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deletePharmacy(Long id) {
        if (!pharmacyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pharmacy not found with id: " + id);
        }
        pharmacyRepository.deleteById(id);
        log.info("Pharmacy deleted with id: {}", id);
    }

    private PharmacyDTO convertToDTO(Pharmacy pharmacy) {
        PharmacyDTO dto = new PharmacyDTO();
        dto.setId(pharmacy.getId());
        dto.setName(pharmacy.getName());
        dto.setAddress(pharmacy.getAddress());
        dto.setCity(pharmacy.getCity());
        dto.setPhoneNumber(pharmacy.getPhoneNumber());
        dto.setEmail(pharmacy.getEmail());
        dto.setOpeningHours(pharmacy.getOpeningHours());

        if (pharmacy.getInventoryItems() != null) {
            dto.setInventoryItemIds(pharmacy.getInventoryItems().stream()
                    .map(inv -> inv.getId())
                    .toList());
        }

        if (pharmacy.getReservations() != null) {
            dto.setReservationIds(pharmacy.getReservations().stream()
                    .map(res -> res.getId())
                    .toList());
        }

        if (pharmacy.getDeliveries() != null) {
            dto.setDeliveryIds(pharmacy.getDeliveries().stream()
                    .map(del -> del.getId())
                    .toList());
        }

        return dto;
    }
}

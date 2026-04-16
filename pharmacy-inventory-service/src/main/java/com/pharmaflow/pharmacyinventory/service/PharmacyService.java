package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.exception.DuplicateResourceException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;

    public PharmacyService(PharmacyRepository pharmacyRepository, ModelMapper modelMapper) {
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<PharmacyDTO> getAllPharmacies() {
        return pharmacyRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
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
        return convertToDTO(saved);
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
        return convertToDTO(updated);
    }

    @Transactional
    public void deletePharmacy(Long id) {
        if (!pharmacyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pharmacy not found with id: " + id);
        }
        pharmacyRepository.deleteById(id);
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

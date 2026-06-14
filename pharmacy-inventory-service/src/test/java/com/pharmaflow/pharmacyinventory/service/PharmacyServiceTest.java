package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.exception.DuplicateResourceException;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PharmacyService pharmacyService;

    private Pharmacy testPharmacy;
    private PharmacyCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        pharmacyService = new PharmacyService(pharmacyRepository, modelMapper, objectMapper, validator);

        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);
        testPharmacy.setName("Apoteka Centar");
        testPharmacy.setAddress("Marsala Tita 10");
        testPharmacy.setCity("Sarajevo");
        testPharmacy.setPhoneNumber("+387-33-123456");
        testPharmacy.setEmail("centar@pharmaflow.ba");
        testPharmacy.setOpeningHours("08:00-20:00");
        testPharmacy.setImageUrl("/demo/pharmacies/pharmaflow-centar.jpg");
        testPharmacy.setInventoryItems(new ArrayList<>());
        testPharmacy.setReservations(new ArrayList<>());
        testPharmacy.setDeliveries(new ArrayList<>());

        testCreateDTO = new PharmacyCreateDTO();
        testCreateDTO.setName("Apoteka Centar");
        testCreateDTO.setAddress("Marsala Tita 10");
        testCreateDTO.setCity("Sarajevo");
        testCreateDTO.setPhoneNumber("+387-33-123456");
        testCreateDTO.setEmail("centar@pharmaflow.ba");
        testCreateDTO.setOpeningHours("08:00-20:00");
        testCreateDTO.setImageUrl("/demo/pharmacies/pharmaflow-centar.jpg");
    }

    @Test
    void findAll_ShouldReturnPageOfPharmacies() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pharmacy> page = new PageImpl<>(List.of(testPharmacy));
        when(pharmacyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PharmacyDTO> result = pharmacyService.findAll(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Apoteka Centar", result.getContent().get(0).getName());
        verify(pharmacyRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPharmacyById_WhenExists_ShouldReturnPharmacy() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));

        PharmacyDTO result = pharmacyService.getPharmacyById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Apoteka Centar", result.getName());
        verify(pharmacyRepository, times(1)).findById(1L);
    }

    @Test
    void getPharmacyById_WhenNotExists_ShouldThrow() {
        when(pharmacyRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> pharmacyService.getPharmacyById(99L));
        assertEquals("Pharmacy not found with id: 99", ex.getMessage());
    }

    @Test
    void createPharmacy_WhenNameNotExists_ShouldReturnCreated() {
        when(pharmacyRepository.existsByName("Apoteka Centar")).thenReturn(false);
        when(pharmacyRepository.save(any(Pharmacy.class))).thenReturn(testPharmacy);

        PharmacyDTO result = pharmacyService.createPharmacy(testCreateDTO);

        assertNotNull(result);
        assertEquals("Apoteka Centar", result.getName());
        assertEquals("/demo/pharmacies/pharmaflow-centar.jpg", result.getImageUrl());
        verify(pharmacyRepository).existsByName("Apoteka Centar");
        verify(pharmacyRepository).save(any(Pharmacy.class));
    }

    @Test
    void createPharmacy_WhenNameExists_ShouldThrowDuplicate() {
        when(pharmacyRepository.existsByName("Apoteka Centar")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> pharmacyService.createPharmacy(testCreateDTO));
        verify(pharmacyRepository, never()).save(any(Pharmacy.class));
    }

    @Test
    void createPharmaciesBatch_ShouldSaveAll() {
        when(pharmacyRepository.existsByName("Apoteka Centar")).thenReturn(false);
        when(pharmacyRepository.saveAll(any())).thenReturn(List.of(testPharmacy));

        List<PharmacyDTO> result = pharmacyService.createPharmaciesBatch(List.of(testCreateDTO));

        assertEquals(1, result.size());
        verify(pharmacyRepository).saveAll(any());
    }

    @Test
    void createPharmaciesBatch_WhenAnyNameExists_ShouldThrow() {
        when(pharmacyRepository.existsByName("Apoteka Centar")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> pharmacyService.createPharmaciesBatch(List.of(testCreateDTO)));
        verify(pharmacyRepository, never()).saveAll(any());
    }

    @Test
    void updatePharmacy_WhenExists_ShouldReturnUpdated() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(pharmacyRepository.save(any(Pharmacy.class))).thenReturn(testPharmacy);

        PharmacyDTO result = pharmacyService.updatePharmacy(1L, testCreateDTO);

        assertNotNull(result);
        verify(pharmacyRepository).save(any(Pharmacy.class));
    }

    @Test
    void updatePharmacy_WhenNotExists_ShouldThrow() {
        when(pharmacyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pharmacyService.updatePharmacy(99L, testCreateDTO));
        verify(pharmacyRepository, never()).save(any(Pharmacy.class));
    }

    @Test
    void patchPharmacy_WithValidPatch_ShouldReturnUpdated() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(pharmacyRepository.existsByName("New Name")).thenReturn(false);
        when(pharmacyRepository.save(any(Pharmacy.class))).thenReturn(testPharmacy);

        String patch = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"New Name\"}]";
        PharmacyDTO result = pharmacyService.patchPharmacy(1L, patch);

        assertNotNull(result);
        verify(pharmacyRepository).save(any(Pharmacy.class));
    }

    @Test
    void patchPharmacy_WithMalformedPatch_ShouldThrowPatchException() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));

        assertThrows(PatchOperationException.class,
                () -> pharmacyService.patchPharmacy(1L, "not a valid patch"));
    }

    @Test
    void patchPharmacy_WithInvalidEmail_ShouldThrowConstraintViolation() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));

        @SuppressWarnings("unchecked")
        ConstraintViolation<PharmacyDTO> violation = mock(ConstraintViolation.class);
        when(validator.validate(any(PharmacyDTO.class))).thenReturn(java.util.Set.of(violation));

        String patch = "[{\"op\":\"replace\",\"path\":\"/email\",\"value\":\"randomemail.com\"}]";

        assertThrows(ConstraintViolationException.class,
                () -> pharmacyService.patchPharmacy(1L, patch));
        verify(pharmacyRepository, never()).save(any(Pharmacy.class));
    }

    @Test
    void patchPharmacy_WhenNotExists_ShouldThrowNotFound() {
        when(pharmacyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pharmacyService.patchPharmacy(99L, "[]"));
    }

    @Test
    void deletePharmacy_WhenExists_ShouldDelete() {
        when(pharmacyRepository.existsById(1L)).thenReturn(true);

        pharmacyService.deletePharmacy(1L);

        verify(pharmacyRepository).deleteById(1L);
    }

    @Test
    void deletePharmacy_WhenNotExists_ShouldThrow() {
        when(pharmacyRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> pharmacyService.deletePharmacy(99L));
        verify(pharmacyRepository, never()).deleteById(anyLong());
    }
}

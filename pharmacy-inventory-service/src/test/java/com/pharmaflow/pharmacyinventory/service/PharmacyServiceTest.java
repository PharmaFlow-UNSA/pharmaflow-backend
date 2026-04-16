package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.exception.DuplicateResourceException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PharmacyService pharmacyService;

    private Pharmacy testPharmacy;
    private PharmacyCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);
        testPharmacy.setName("Apoteka Centar");
        testPharmacy.setAddress("Marsala Tita 10");
        testPharmacy.setCity("Sarajevo");
        testPharmacy.setPhoneNumber("+387-33-123456");
        testPharmacy.setEmail("centar@pharmaflow.ba");
        testPharmacy.setOpeningHours("08:00-20:00");
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
    }

    @Test
    void getAllPharmacies_ShouldReturnListOfPharmacies() {
        // Arrange
        List<Pharmacy> pharmacies = List.of(testPharmacy);
        when(pharmacyRepository.findAll()).thenReturn(pharmacies);

        // Act
        List<PharmacyDTO> result = pharmacyService.getAllPharmacies();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Apoteka Centar", result.get(0).getName());
        verify(pharmacyRepository, times(1)).findAll();
    }

    @Test
    void getPharmacyById_WhenPharmacyExists_ShouldReturnPharmacy() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));

        // Act
        PharmacyDTO result = pharmacyService.getPharmacyById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Apoteka Centar", result.getName());
        assertEquals("Marsala Tita 10", result.getAddress());
        assertEquals("Sarajevo", result.getCity());
        verify(pharmacyRepository, times(1)).findById(1L);
    }

    @Test
    void getPharmacyById_WhenPharmacyNotExists_ShouldThrowException() {
        // Arrange
        when(pharmacyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> pharmacyService.getPharmacyById(99L));
        assertEquals("Pharmacy not found with id: 99", exception.getMessage());
        verify(pharmacyRepository, times(1)).findById(99L);
    }

    @Test
    void createPharmacy_WhenNameNotExists_ShouldReturnCreatedPharmacy() {
        // Arrange
        when(pharmacyRepository.existsByName("Apoteka Centar")).thenReturn(false);
        when(pharmacyRepository.save(any(Pharmacy.class))).thenReturn(testPharmacy);

        // Act
        PharmacyDTO result = pharmacyService.createPharmacy(testCreateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Apoteka Centar", result.getName());
        assertEquals("Marsala Tita 10", result.getAddress());
        assertEquals("Sarajevo", result.getCity());
        verify(pharmacyRepository, times(1)).existsByName("Apoteka Centar");
        verify(pharmacyRepository, times(1)).save(any(Pharmacy.class));
    }

    @Test
    void createPharmacy_WhenNameExists_ShouldThrowDuplicateException() {
        // Arrange
        when(pharmacyRepository.existsByName("Apoteka Centar")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> pharmacyService.createPharmacy(testCreateDTO));
        assertEquals("Pharmacy with name 'Apoteka Centar' already exists", exception.getMessage());
        verify(pharmacyRepository, times(1)).existsByName("Apoteka Centar");
        verify(pharmacyRepository, never()).save(any(Pharmacy.class));
    }

    @Test
    void updatePharmacy_WhenPharmacyExists_ShouldReturnUpdatedPharmacy() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(pharmacyRepository.save(any(Pharmacy.class))).thenReturn(testPharmacy);

        // Act
        PharmacyDTO result = pharmacyService.updatePharmacy(1L, testCreateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Apoteka Centar", result.getName());
        verify(pharmacyRepository, times(1)).findById(1L);
        verify(pharmacyRepository, times(1)).save(any(Pharmacy.class));
    }

    @Test
    void updatePharmacy_WhenPharmacyNotExists_ShouldThrowException() {
        // Arrange
        when(pharmacyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> pharmacyService.updatePharmacy(99L, testCreateDTO));
        assertEquals("Pharmacy not found with id: 99", exception.getMessage());
        verify(pharmacyRepository, times(1)).findById(99L);
        verify(pharmacyRepository, never()).save(any(Pharmacy.class));
    }

    @Test
    void deletePharmacy_WhenPharmacyExists_ShouldDeletePharmacy() {
        // Arrange
        when(pharmacyRepository.existsById(1L)).thenReturn(true);

        // Act
        pharmacyService.deletePharmacy(1L);

        // Assert
        verify(pharmacyRepository, times(1)).existsById(1L);
        verify(pharmacyRepository, times(1)).deleteById(1L);
    }

    @Test
    void deletePharmacy_WhenPharmacyNotExists_ShouldThrowException() {
        // Arrange
        when(pharmacyRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> pharmacyService.deletePharmacy(99L));
        assertEquals("Pharmacy not found with id: 99", exception.getMessage());
        verify(pharmacyRepository, times(1)).existsById(99L);
        verify(pharmacyRepository, never()).deleteById(anyLong());
    }
}

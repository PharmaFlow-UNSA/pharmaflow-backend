package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.models.Reservation;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import com.pharmaflow.pharmacyinventory.repositories.ReservationRepository;
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
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation testReservation;
    private ReservationDTO testReservationDTO;
    private Pharmacy testPharmacy;

    @BeforeEach
    void setUp() {
        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);
        testPharmacy.setName("Test Pharmacy");

        LocalDateTime now = LocalDateTime.now();

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUserId(1L);
        testReservation.setProductId(100L);
        testReservation.setQuantity(1);
        testReservation.setStatus("PENDING");
        testReservation.setReservedAt(now);
        testReservation.setExpiresAt(now.plusDays(1));
        testReservation.setPharmacy(testPharmacy);

        testReservationDTO = new ReservationDTO();
        testReservationDTO.setId(1L);
        testReservationDTO.setUserId(1L);
        testReservationDTO.setProductId(100L);
        testReservationDTO.setQuantity(1);
        testReservationDTO.setStatus("PENDING");
        testReservationDTO.setReservedAt(now);
        testReservationDTO.setExpiresAt(now.plusDays(1));
        testReservationDTO.setPharmacyId(1L);
    }

    @Test
    void getAllReservations_ShouldReturnListOfReservations() {
        // Arrange
        List<Reservation> reservations = List.of(testReservation);
        when(reservationRepository.findAll()).thenReturn(reservations);

        // Act
        List<ReservationDTO> result = reservationService.getAllReservations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals("PENDING", result.get(0).getStatus());
        verify(reservationRepository, times(1)).findAll();
    }

    @Test
    void getReservationById_WhenReservationExists_ShouldReturnReservation() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // Act
        ReservationDTO result = reservationService.getReservationById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(100L, result.getProductId());
        assertEquals(1, result.getQuantity());
        assertEquals("PENDING", result.getStatus());
        assertEquals(1L, result.getPharmacyId());
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    void getReservationById_WhenReservationNotExists_ShouldThrowException() {
        // Arrange
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> reservationService.getReservationById(99L));
        assertEquals("Reservation not found with id: 99", exception.getMessage());
        verify(reservationRepository, times(1)).findById(99L);
    }

    @Test
    void getReservationsByUserId_ShouldReturnListOfReservations() {
        // Arrange
        List<Reservation> reservations = List.of(testReservation);
        when(reservationRepository.findByUserId(1L)).thenReturn(reservations);

        // Act
        List<ReservationDTO> result = reservationService.getReservationsByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(100L, result.get(0).getProductId());
        verify(reservationRepository, times(1)).findByUserId(1L);
    }

    @Test
    void createReservation_WhenPharmacyExists_ShouldReturnCreatedReservation() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // Act
        ReservationDTO result = reservationService.createReservation(testReservationDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(100L, result.getProductId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(1L, result.getPharmacyId());
        verify(pharmacyRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void createReservation_WhenPharmacyNotExists_ShouldThrowException() {
        // Arrange
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> reservationService.createReservation(testReservationDTO));
        assertEquals("Pharmacy not found with id: 1", exception.getMessage());
        verify(pharmacyRepository, times(1)).findById(1L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void deleteReservation_WhenReservationExists_ShouldDeleteReservation() {
        // Arrange
        when(reservationRepository.existsById(1L)).thenReturn(true);

        // Act
        reservationService.deleteReservation(1L);

        // Assert
        verify(reservationRepository, times(1)).existsById(1L);
        verify(reservationRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteReservation_WhenReservationNotExists_ShouldThrowException() {
        // Arrange
        when(reservationRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> reservationService.deleteReservation(99L));
        assertEquals("Reservation not found with id: 99", exception.getMessage());
        verify(reservationRepository, times(1)).existsById(99L);
        verify(reservationRepository, never()).deleteById(anyLong());
    }
}

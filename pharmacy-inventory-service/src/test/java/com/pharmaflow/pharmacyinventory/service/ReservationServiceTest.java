package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.messaging.saga.ReservationRequestedEvent;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.models.Reservation;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import com.pharmaflow.pharmacyinventory.repositories.ReservationRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private ReservationService reservationService;

    private Pharmacy testPharmacy;
    private Reservation testReservation;
    private ReservationDTO testReservationDTO;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, pharmacyRepository, modelMapper, objectMapper, validator);

        testPharmacy = new Pharmacy();
        testPharmacy.setId(1L);

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUserId(10L);
        testReservation.setProductId(100L);
        testReservation.setQuantity(2);
        testReservation.setStatus("PENDING");
        testReservation.setReservedAt(LocalDateTime.of(2026, 5, 5, 10, 0));
        testReservation.setPharmacy(testPharmacy);

        testReservationDTO = new ReservationDTO();
        testReservationDTO.setUserId(10L);
        testReservationDTO.setProductId(100L);
        testReservationDTO.setQuantity(2);
        testReservationDTO.setStatus("PENDING");
        testReservationDTO.setReservedAt(LocalDateTime.of(2026, 5, 5, 10, 0));
        testReservationDTO.setPharmacyId(1L);
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = new PageImpl<>(List.of(testReservation));
        when(reservationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ReservationDTO> result = reservationService.findAll(null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getReservationById_WhenExists_ShouldReturn() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        ReservationDTO result = reservationService.getReservationById(1L);

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void getReservationById_WhenNotExists_ShouldThrow() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.getReservationById(99L));
    }

    @Test
    void getReservationsByPharmacyId_ShouldReturnList() {
        when(reservationRepository.findByPharmacyId(1L)).thenReturn(List.of(testReservation));

        assertEquals(1, reservationService.getReservationsByPharmacyId(1L).size());
    }

    @Test
    void getReservationsByUserId_ShouldReturnList() {
        when(reservationRepository.findByUserId(10L)).thenReturn(List.of(testReservation));

        assertEquals(1, reservationService.getReservationsByUserId(10L).size());
    }

    @Test
    void getReservationsByStatus_ShouldReturnList() {
        when(reservationRepository.findByStatus("PENDING")).thenReturn(List.of(testReservation));

        assertEquals(1, reservationService.getReservationsByStatus("PENDING").size());
    }

    @Test
    void createReservation_WhenPharmacyExists_ShouldSucceed() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationDTO result = reservationService.createReservation(testReservationDTO);

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void createReservation_WhenPharmacyNotExists_ShouldThrow() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.createReservation(testReservationDTO));
    }

    @Test
    void createReservationForSaga_WhenNewCorrelation_ShouldCreatePendingReservation() {
        when(reservationRepository.findBySagaCorrelationId("corr-1")).thenReturn(Optional.empty());
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(55L);
            return reservation;
        });

        ReservationDTO result = reservationService.createReservationForSaga(
                new ReservationRequestedEvent(
                        "corr-1", 1L, 10L, null, 100L, 1L, 2, LocalDateTime.now(), null));

        assertEquals(55L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("corr-1", result.getSagaCorrelationId());
    }

    @Test
    void createReservationForSaga_WhenDuplicateCorrelation_ShouldReturnExistingReservation() {
        testReservation.setSagaCorrelationId("corr-1");
        when(reservationRepository.findBySagaCorrelationId("corr-1")).thenReturn(Optional.of(testReservation));

        ReservationDTO result = reservationService.createReservationForSaga(
                new ReservationRequestedEvent(
                        "corr-1", 1L, 10L, null, 100L, 1L, 2, LocalDateTime.now(), null));

        assertEquals(1L, result.getId());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void compensateReservationForSaga_ShouldCancelReservation() {
        testReservation.setSagaCorrelationId("corr-1");
        when(reservationRepository.findBySagaCorrelationId("corr-1")).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationDTO result = reservationService.compensateReservationForSaga("corr-1");

        assertEquals("CANCELLED", result.getStatus());
    }

    @Test
    void createReservationsBatch_ShouldSaveAll() {
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(testPharmacy));
        when(reservationRepository.saveAll(any())).thenReturn(List.of(testReservation));

        List<ReservationDTO> result = reservationService.createReservationsBatch(List.of(testReservationDTO));

        assertEquals(1, result.size());
    }

    @Test
    void updateReservation_WhenExists_ShouldUpdate() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationDTO result = reservationService.updateReservation(1L, testReservationDTO);

        assertNotNull(result);
    }

    @Test
    void updateReservation_WhenNotExists_ShouldThrow() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.updateReservation(99L, testReservationDTO));
    }

    @Test
    void patchReservation_WithValidPatch_ShouldUpdate() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationDTO result = reservationService.patchReservation(1L,
                "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"READY\"}]");

        assertNotNull(result);
    }

    @Test
    void patchReservation_WithMalformedPatch_ShouldThrow() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        assertThrows(PatchOperationException.class,
                () -> reservationService.patchReservation(1L, "junk"));
    }

    @Test
    void deleteReservation_WhenExists_ShouldDelete() {
        when(reservationRepository.existsById(1L)).thenReturn(true);

        reservationService.deleteReservation(1L);

        verify(reservationRepository).deleteById(1L);
    }

    @Test
    void deleteReservation_WhenNotExists_ShouldThrow() {
        when(reservationRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.deleteReservation(99L));
    }
}

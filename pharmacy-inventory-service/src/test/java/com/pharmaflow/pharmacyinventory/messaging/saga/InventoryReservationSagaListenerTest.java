package com.pharmaflow.pharmacyinventory.messaging.saga;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.ReservationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryReservationSagaListenerTest {

    @Mock private ReservationService reservationService;
    @Mock private InventorySagaEventPublisher eventPublisher;

    private InventoryReservationSagaListener listener;

    @BeforeEach
    void setUp() {
        listener = new InventoryReservationSagaListener(reservationService, eventPublisher);
    }

    @Test
    void onReservationRequestedShouldPublishCreatedEvent() {
        ReservationDTO reservation = new ReservationDTO();
        reservation.setId(55L);
        reservation.setStatus("PENDING");
        when(reservationService.createReservationForSaga(any())).thenReturn(reservation);

        listener.onReservationRequested(requestedEvent());

        verify(eventPublisher).publishReservationCreated(any(ReservationCreatedEvent.class));
    }

    @Test
    void onReservationRequestedShouldPublishRejectedEventWhenCreationFails() {
        when(reservationService.createReservationForSaga(any()))
                .thenThrow(new ResourceNotFoundException("Pharmacy not found"));

        listener.onReservationRequested(requestedEvent());

        verify(eventPublisher).publishReservationRejected(any(ReservationRejectedEvent.class));
    }

    @Test
    void onReservationCompensationRequestedShouldPublishCompensatedEvent() {
        ReservationDTO reservation = new ReservationDTO();
        reservation.setId(55L);
        when(reservationService.compensateReservationForSaga("corr-1")).thenReturn(reservation);

        listener.onReservationCompensationRequested(
                new ReservationCompensationRequestedEvent(
                        "corr-1", 55L, "rollback", LocalDateTime.now()));

        verify(eventPublisher).publishReservationCompensated(any(ReservationCompensatedEvent.class));
    }

    private ReservationRequestedEvent requestedEvent() {
        return new ReservationRequestedEvent(
                "corr-1", 1L, 10L, null, 100L, 1L, 2, LocalDateTime.now(), null);
    }
}

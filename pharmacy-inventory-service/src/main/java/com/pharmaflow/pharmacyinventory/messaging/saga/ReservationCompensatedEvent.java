package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;

public record ReservationCompensatedEvent(
        String correlationId, Long reservationId, LocalDateTime compensatedAt) {
}

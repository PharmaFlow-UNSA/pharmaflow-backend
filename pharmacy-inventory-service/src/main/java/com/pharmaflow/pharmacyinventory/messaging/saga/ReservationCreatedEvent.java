package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;

public record ReservationCreatedEvent(
        String correlationId, Long reservationId, String status, LocalDateTime createdAt) {
}

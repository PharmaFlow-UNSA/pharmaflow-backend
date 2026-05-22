package com.pharmaflow.pharmacyinventory.messaging.saga;

import java.time.LocalDateTime;

public record ReservationRejectedEvent(
        String correlationId, String reason, LocalDateTime rejectedAt) {
}

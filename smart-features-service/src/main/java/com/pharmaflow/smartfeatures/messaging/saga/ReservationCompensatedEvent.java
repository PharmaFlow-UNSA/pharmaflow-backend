package com.pharmaflow.smartfeatures.messaging.saga;

import java.time.LocalDateTime;

public record ReservationCompensatedEvent(
    String correlationId, Long reservationId, LocalDateTime compensatedAt) {}

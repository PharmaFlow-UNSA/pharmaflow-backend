package com.pharmaflow.smartfeatures.messaging.saga;

import java.time.LocalDateTime;

public record ReservationCompensationRequestedEvent(
    String correlationId, Long reservationId, String reason, LocalDateTime requestedAt) {}

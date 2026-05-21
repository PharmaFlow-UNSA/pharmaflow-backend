package com.pharmaflow.smartfeatures.messaging.saga;

import java.time.LocalDateTime;

public record ReservationRequestedEvent(
    String correlationId,
    Long recommendationId,
    Long userId,
    Long patientProfileId,
    Long productId,
    Long pharmacyId,
    Integer quantity,
    LocalDateTime requestedAt,
    LocalDateTime expiresAt) {}

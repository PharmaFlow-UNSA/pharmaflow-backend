package com.pharmaflow.smartfeatures.messaging.saga;

import java.time.LocalDateTime;

public record ReservationRejectedEvent(
    String correlationId, String reason, LocalDateTime rejectedAt) {}

package com.pharmaflow.smartfeatures.messaging.saga;

import java.time.LocalDateTime;

public record RecommendationReservationFinalizedEvent(
    String correlationId, Long recommendationId, Long reservationId, LocalDateTime finalizedAt) {}

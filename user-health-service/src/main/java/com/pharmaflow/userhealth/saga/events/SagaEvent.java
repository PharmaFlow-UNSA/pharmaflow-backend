package com.pharmaflow.userhealth.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base event for all saga events.
 * Contains common fields for event tracking and correlation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class SagaEvent {

    /**
     * Unique event ID
     */
    private String eventId = UUID.randomUUID().toString();

    /**
     * Saga transaction ID - correlates all events in a saga
     */
    private String sagaId;

    /**
     * Event timestamp
     */
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Event type
     */
    private String eventType;

    /**
     * Source service that emitted the event
     */
    private String sourceService;
}


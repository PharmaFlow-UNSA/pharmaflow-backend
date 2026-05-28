package com.pharmaflow.producthealth.saga.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity that tracks the state of a product creation saga transaction.
 * Used for monitoring, debugging, and ensuring idempotency.
 */
@Entity
@Table(name = "product_saga_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique saga transaction ID */
    @Column(unique = true, nullable = false)
    private String sagaId;

    /** Current saga status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    /** Type of saga */
    @Column(nullable = false)
    private String sagaType;

    /** Product ID */
    private Long entityId;

    /** Current step in the saga */
    private String currentStep;

    /** Payload data (JSON) */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /** Error message if saga failed */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SagaStatus {
        STARTED,           // Saga initiated
        PRODUCT_CREATED,   // Local transaction 1 completed - product saved
        STOCK_PENDING,     // Waiting for pharmacy inventory response
        COMPLETED,         // All steps successful - saga is final
        COMPENSATING,      // Rolling back
        COMPENSATED,       // Rollback completed
        FAILED             // Saga failed permanently
    }
}

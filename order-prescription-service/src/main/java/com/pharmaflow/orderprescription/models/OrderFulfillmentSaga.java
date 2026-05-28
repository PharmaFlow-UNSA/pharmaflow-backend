package com.pharmaflow.orderprescription.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persisted state for an order-fulfillment saga. One row per saga run, keyed
 * by {@code correlationId}, so we can track progress, debug runs and stay
 * idempotent when reply messages are redelivered.
 */
@Entity
@Table(
        name = "order_fulfillment_saga",
        indexes = {
                @Index(name = "idx_order_saga_correlation_id", columnList = "correlation_id", unique = true),
                @Index(name = "idx_order_saga_order_id", columnList = "order_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFulfillmentSaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 36)
    private String correlationId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "pharmacy_id")
    private Long pharmacyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "current_step", length = 64)
    private String currentStep;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        STARTED,           // initial — order saved locally
        STOCK_PENDING,     // OrderPlacedEvent published, waiting for reply
        COMPLETED,         // FINAL — stock reserved successfully
        COMPENSATING,      // rolling back after stock rejection
        COMPENSATED,       // FINAL — rollback finished
        CANCELLING,        // user/order cancelled an already-confirmed order
        CANCELLED,         // FINAL — order cancelled and stock restored
        FAILED             // FINAL — saga failed irrecoverably
    }
}

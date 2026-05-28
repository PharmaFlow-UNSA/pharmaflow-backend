package com.pharmaflow.pharmacyinventory.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the stock that pharmacy-inventory-service deducted for a particular
 * order-fulfillment saga, keyed by {@code correlationId}. We persist this so
 * that, if a compensating {@code order.cancelled} event arrives later, we know
 * exactly what to restock — and so the reservation is idempotent across
 * redelivery of the inbound {@code order.placed} event.
 */
@Entity
@Table(
        name = "order_stock_reservations",
        indexes = {
                @Index(name = "idx_order_stock_correlation_id", columnList = "correlation_id", unique = true),
                @Index(name = "idx_order_stock_order_id", columnList = "order_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 36)
    private String correlationId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "pharmacy_id", nullable = false)
    private Long pharmacyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private List<Item> items = new ArrayList<>();

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
        RESERVED,    // stock has been deducted for this correlationId
        REJECTED,    // failure path — nothing was deducted (transaction rolled back)
        RESTOCKED    // FINAL — stock has been put back after a cancellation
    }

    @Entity
    @Table(name = "order_stock_reservation_items")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "product_id", nullable = false)
        private Long productId;

        @Column(nullable = false)
        private Integer quantity;

        @Column(name = "inventory_id", nullable = false)
        private Long inventoryId;

        @ToString.Exclude
        @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "reservation_id", nullable = false)
        private OrderStockReservation reservation;
    }
}

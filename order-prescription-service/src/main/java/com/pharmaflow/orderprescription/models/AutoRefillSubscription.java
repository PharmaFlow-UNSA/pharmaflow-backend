package com.pharmaflow.orderprescription.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "auto_refill_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoRefillSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer dosagePerDay;

    @Column(nullable = false)
    private Integer tabletsPerPackage;

    @Column(nullable = false)
    private Integer intervalDays;

    @Column(nullable = false)
    private LocalDate nextOrderDate;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String shippingAddress;
}

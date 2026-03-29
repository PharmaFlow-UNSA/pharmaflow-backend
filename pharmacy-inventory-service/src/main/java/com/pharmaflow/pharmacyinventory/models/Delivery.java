package com.pharmaflow.pharmacyinventory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "pharmacy")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String deliveryAddress;

    @Column(nullable = false)
    private String status;

    private LocalDateTime estimatedDelivery;

    private LocalDateTime actualDelivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;
}

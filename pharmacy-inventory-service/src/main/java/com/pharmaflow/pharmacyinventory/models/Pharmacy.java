package com.pharmaflow.pharmacyinventory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pharmacies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private String phoneNumber;

    private String email;

    private String openingHours;

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Inventory> inventoryItems = new ArrayList<>();

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Delivery> deliveries = new ArrayList<>();
}

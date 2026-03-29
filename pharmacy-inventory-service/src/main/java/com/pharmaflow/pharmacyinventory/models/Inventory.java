package com.pharmaflow.pharmacyinventory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "inventories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "pharmacy")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    private Integer reorderLevel;

    private LocalDate lastRestocked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;
}

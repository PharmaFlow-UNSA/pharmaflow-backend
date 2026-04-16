package com.pharmaflow.orderprescription.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"orders", "autoRefillSubscriptions"})
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime reviewedAt;

    private String reviewerNotes;

    @OneToMany(mappedBy = "prescription", fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "prescription", fetch = FetchType.LAZY)
    private List<AutoRefillSubscription> autoRefillSubscriptions = new ArrayList<>();
}

package com.pharmaflow.userhealth.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "therapies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Therapy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medicationName;

    private String dosage;

    private String frequency;
}
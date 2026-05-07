package com.pharmaflow.userhealth.models;

import com.pharmaflow.userhealth.models.enums.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "allergies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String allergen;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Severity severity;

    private String activeSubstance;
}
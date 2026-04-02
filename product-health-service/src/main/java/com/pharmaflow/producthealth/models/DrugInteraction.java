package com.pharmaflow.producthealth.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drug_interactions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"substance_a_id", "substance_b_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prva supstanca u interakciji
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substance_a_id", nullable = false)
    private Substance substanceA;

    // Druga supstanca u interakciji
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substance_b_id", nullable = false)
    private Substance substanceB;

    // Nivo ozbiljnosti interakcije
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityLevel severity;

    // Opis interakcije (šta se dešava, npr. "Pojačano krvarenje")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // Klinička preporuka (npr. "Izbjegavati kombinaciju", "Pratiti INR")
    @Column(name = "clinical_recommendation", columnDefinition = "TEXT")
    private String clinicalRecommendation;

    public enum SeverityLevel {
        MINOR,      // Manja, uglavnom bez kliničkog značaja
        MODERATE,   // Umjerena, treba pratiti
        MAJOR,      // Ozbiljna, izbjegavati kombinaciju
        CONTRAINDICATED // Apsolutno kontraindikovana kombinacija
    }
}
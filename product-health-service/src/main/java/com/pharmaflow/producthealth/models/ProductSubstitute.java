package com.pharmaflow.producthealth.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_substitutes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"original_product_id", "substitute_product_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSubstitute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Originalni lijek (npr. Brufen 400mg)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_product_id", nullable = false)
    private Product originalProduct;

    // Zamjenski lijek (npr. generički Ibuprofen 400mg)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_product_id", nullable = false)
    private Product substituteProduct;

    // Tip zamjene
    @Enumerated(EnumType.STRING)
    @Column(name = "substitute_type", nullable = false)
    private SubstituteType substituteType;

    // Da li je terapijski ekvivalent (ista doza, ista supstanca)
    @Column(name = "is_therapeutic_equivalent", nullable = false)
    private Boolean isTherapeuticEquivalent = false;

    // Napomena (npr. "Niža cijena", "Dostupno bez recepta")
    private String note;

    public enum SubstituteType {
        GENERIC,        // Generička verzija istog lijeka
        THERAPEUTIC,    // Terapijski ekvivalent (ista indikacija, drugačija supstanca)
        BIOSIMILAR      // Biosimilar (za biološke lijekove)
    }
}
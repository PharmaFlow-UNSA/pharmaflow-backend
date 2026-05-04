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

    // Substitute product (e.g. generic Ibuprofen 400mg)
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

    // Note (e.g. "Lower price", "Available without prescription")
    private String note;

    public enum SubstituteType {
        GENERIC,        // Generic version of the same drug
        THERAPEUTIC,    // Therapeutic equivalent (same indication, different substance)
        BIOSIMILAR      // Biosimilar (for biological drugs)
    }
}
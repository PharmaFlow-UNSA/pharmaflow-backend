package com.pharmaflow.smartfeatures.model.symptom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps a symptom to products that may be relevant for it.
 */
@Entity
@Table(
        name = "symptom_product_match",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_symptom_product_match_symptom_product",
                    columnNames = {"symptom_id", "product_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomProductMatch {

    /** Primary key of the match record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id", nullable = false, updatable = false)
    private Long matchId;

    /** Symptom that the match belongs to. */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "symptom_id", nullable = false)
    private Symptom symptom;

    /** External reference to the matched product. */
    @NotNull
    @Positive
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** Match strength for ranking results. */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "relevance_score")
    private Double relevanceScore;

    /** Short explanation of why the product was matched. */
    @Size(max = 500)
    @Column(name = "match_reason", length = 500)
    private String matchReason;
}

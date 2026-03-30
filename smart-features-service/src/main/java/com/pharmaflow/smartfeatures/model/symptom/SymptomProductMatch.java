package com.pharmaflow.smartfeatures.model.symptom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps a symptom to products that may be relevant for it.
 */
@Entity
@Table(name = "symptom_product_match")
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
    @ManyToOne
    @JoinColumn(name = "symptom_id", nullable = false)
    private Symptom symptom;

    /** External reference to the matched product. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** Match strength for ranking results. */
    @Column(name = "relevance_score")
    private Double relevanceScore;

    /** Short explanation of why the product was matched. */
    @Column(name = "match_reason")
    private String matchReason;
}

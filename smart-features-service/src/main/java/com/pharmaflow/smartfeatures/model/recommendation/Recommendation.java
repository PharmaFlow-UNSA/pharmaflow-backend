package com.pharmaflow.smartfeatures.model.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a product recommendation generated for a user or patient profile.
 */
@Entity
@Table(name = "recommendation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    /** Primary key of the recommendation record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id", nullable = false, updatable = false)
    private Long recommendationId;

    /** External reference to the user. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Optional reference to the patient profile. */
    @Column(name = "patient_profile_id")
    private Long patientProfileId;

    /** External reference to the product. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** Strategy used to generate the recommendation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false)
    private RecommendationType recommendationType;

    /** Ranking or confidence value for the recommendation. */
    @Column(name = "score")
    private Double score;

    /** Human-readable explanation of why the recommendation was generated. */
    @Column(name = "reason_text")
    private String reasonText;

    /** Timestamp when the recommendation was generated. */
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /** Optional timestamp after which the recommendation is no longer valid. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Current lifecycle state of the recommendation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status;

    /** Interaction events linked to this recommendation. */
    @Default
    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendationEvent> events = new ArrayList<>();
}

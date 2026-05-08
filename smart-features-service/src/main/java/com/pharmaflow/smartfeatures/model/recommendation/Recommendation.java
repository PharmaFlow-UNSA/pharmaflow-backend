package com.pharmaflow.smartfeatures.model.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.util.RecommendationKeyNormalizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents a product recommendation generated for a user or patient profile. */
@Entity
@Table(
    name = "recommendation",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_recommendation_active_dimension_key",
          columnNames = {"active_dimension_key"})
    })
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
  @NotNull
  @Positive
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** Optional reference to the patient profile. */
  @Positive
  @Column(name = "patient_profile_id")
  private Long patientProfileId;

  /** External reference to the product. */
  @NotNull
  @Positive
  @Column(name = "product_id", nullable = false)
  private Long productId;

  /** Strategy used to generate the recommendation. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "recommendation_type", nullable = false)
  private RecommendationType recommendationType;

  /** Ranking or confidence value for the recommendation. */
  @DecimalMin("0.0")
  @DecimalMax("1.0")
  @Column(name = "score")
  private Double score;

  /** Human-readable explanation of why the recommendation was generated. */
  @Size(max = 500)
  @Column(name = "reason_text", length = 500)
  private String reasonText;

  /** Timestamp when the recommendation was generated. */
  @NotNull
  @Column(name = "generated_at", nullable = false)
  private LocalDateTime generatedAt;

  /** Optional timestamp after which the recommendation is no longer valid. */
  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  /** Current lifecycle state of the recommendation. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private RecommendationStatus status;

  /** Unique key used to prevent duplicate ACTIVE recommendations across concurrent writes. */
  @Size(max = 200)
  @Column(name = "active_dimension_key", length = 200)
  private String activeDimensionKey;

  /** Interaction events linked to this recommendation. */
  @Default
  @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendationEvent> events = new ArrayList<>();

  @PrePersist
  @PreUpdate
  void syncActiveDimensionKey() {
    activeDimensionKey =
        RecommendationKeyNormalizer.buildActiveDimensionKey(
            userId, patientProfileId, productId, recommendationType, status);
  }
}

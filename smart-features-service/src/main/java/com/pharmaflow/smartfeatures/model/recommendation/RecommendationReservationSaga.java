package com.pharmaflow.smartfeatures.model.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationReservationSagaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recommendation_reservation_saga")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationReservationSaga {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "saga_id", nullable = false, updatable = false)
  private Long sagaId;

  @NotBlank
  @Size(max = 36)
  @Column(name = "correlation_id", nullable = false, unique = true, length = 36)
  private String correlationId;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "recommendation_id", nullable = false)
  private Recommendation recommendation;

  @NotNull
  @Positive
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Positive
  @Column(name = "patient_profile_id")
  private Long patientProfileId;

  @NotNull
  @Positive
  @Column(name = "product_id", nullable = false)
  private Long productId;

  @NotNull
  @Positive
  @Column(name = "pharmacy_id", nullable = false)
  private Long pharmacyId;

  @NotNull
  @Positive
  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Positive
  @Column(name = "reservation_id")
  private Long reservationId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private RecommendationReservationSagaStatus status;

  @Size(max = 500)
  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @NotNull
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}

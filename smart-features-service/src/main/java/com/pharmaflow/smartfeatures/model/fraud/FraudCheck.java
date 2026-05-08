package com.pharmaflow.smartfeatures.model.fraud;

import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents a fraud evaluation performed for a user or order. */
@Entity
@Table(name = "fraud_check")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheck {

  /** Primary key of the fraud check. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "fraud_check_id", nullable = false, updatable = false)
  private Long fraudCheckId;

  /** External reference to the user being evaluated. */
  @NotNull
  @Positive
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** External reference to the related order. */
  @NotNull
  @Positive
  @Column(name = "order_id", nullable = false)
  private Long orderId;

  /** Numeric risk value calculated by the fraud process. */
  @DecimalMin("0.0")
  @DecimalMax("100.0")
  @Column(name = "risk_score")
  private Double riskScore;

  /** Final decision produced by the fraud check. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "decision", nullable = false)
  private FraudDecision decision;

  /** Timestamp when the fraud check was completed. */
  @NotNull
  @Column(name = "checked_at", nullable = false)
  private LocalDateTime checkedAt;

  /** Audit logs produced while evaluating this check. */
  @Default
  @OneToMany(mappedBy = "fraudCheck", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FraudLog> fraudLogs = new ArrayList<>();
}

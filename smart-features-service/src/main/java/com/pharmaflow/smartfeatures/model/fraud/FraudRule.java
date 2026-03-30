package com.pharmaflow.smartfeatures.model.fraud;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a fraud rule used to score or explain fraud checks.
 */
@Entity
@Table(name = "fraud_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRule {

    /** Primary key of the fraud rule. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id", nullable = false, updatable = false)
    private Long ruleId;

    /** Human-readable name of the rule. */
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    /** Short explanation of what the rule checks. */
    @Column(name = "description")
    private String description;

    /** Relative score impact of the rule. */
    @Column(name = "weight")
    private Double weight;

    /** Indicates whether the rule is currently enabled. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** Fraud logs that reference this rule. */
    @Default
    @OneToMany(mappedBy = "fraudRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FraudLog> fraudLogs = new ArrayList<>();
}

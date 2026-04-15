package com.pharmaflow.smartfeatures.model.fraud;

import com.pharmaflow.smartfeatures.util.TextSanitizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(
        name = "fraud_rule",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_fraud_rule_normalized_name", columnNames = "normalized_rule_name")
        })
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
    @NotBlank
    @Size(max = 100)
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /** Normalized rule name used for duplicate detection. */
    @NotBlank
    @Size(max = 100)
    @Column(name = "normalized_rule_name", nullable = false, length = 100)
    private String normalizedRuleName;

    /** Short explanation of what the rule checks. */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /** Relative score impact of the rule. */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("100.0")
    @Column(name = "weight")
    private Double weight;

    /** Indicates whether the rule is currently enabled. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** Fraud logs that reference this rule. */
    @Default
    @OneToMany(mappedBy = "fraudRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FraudLog> fraudLogs = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        ruleName = TextSanitizer.sanitizeRequiredText(ruleName);
        description = TextSanitizer.sanitizeOptionalText(description);
        normalizedRuleName = TextSanitizer.normalizeKey(ruleName);
    }
}

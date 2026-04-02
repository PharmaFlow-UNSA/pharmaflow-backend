package com.pharmaflow.smartfeatures.model.fraud;

import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an audit log entry produced during fraud evaluation.
 */
@Entity
@Table(name = "fraud_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudLog {

    /** Primary key of the fraud log entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fraud_log_id", nullable = false, updatable = false)
    private Long fraudLogId;

    /** Parent fraud check that produced the log. */
    @ManyToOne
    @JoinColumn(name = "fraud_check_id", nullable = false)
    private FraudCheck fraudCheck;

    /** Fraud rule referenced by the log entry. */
    @ManyToOne
    @JoinColumn(name = "rule_id", nullable = false)
    private FraudRule fraudRule;

    /** Type of fraud event that was recorded. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private FraudEventType eventType;

    /** Additional details explaining the logged event. */
    @Column(name = "details")
    private String details;

    /** Timestamp when the fraud log was created. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

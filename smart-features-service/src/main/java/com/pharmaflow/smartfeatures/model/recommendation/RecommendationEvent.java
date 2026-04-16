package com.pharmaflow.smartfeatures.model.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
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
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user or system interaction with a recommendation.
 */
@Entity
@Table(name = "recommendation_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationEvent {

    /** Primary key of the recommendation event. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false, updatable = false)
    private Long eventId;

    /** Parent recommendation that this event belongs to. */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    /** Type of recorded interaction. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private RecommendationEventType eventType;

    /** Timestamp when the event occurred. */
    @NotNull
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
}

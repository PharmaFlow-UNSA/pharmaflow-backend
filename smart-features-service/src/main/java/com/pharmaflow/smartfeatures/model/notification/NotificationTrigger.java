package com.pharmaflow.smartfeatures.model.notification;

import com.pharmaflow.smartfeatures.enums.notification.NotificationTriggerSource;
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
 * Represents the cause that triggered a notification.
 */
@Entity
@Table(name = "notification_trigger")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTrigger {

    /** Primary key of the trigger record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trigger_id", nullable = false, updatable = false)
    private Long triggerId;

    /** Notification associated with this trigger. */
    @ManyToOne
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    /** Source type that caused the notification. */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", nullable = false)
    private NotificationTriggerSource triggerSource;

    /** External reference to the source record when available. */
    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    /** Timestamp when the notification was triggered. */
    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;
}

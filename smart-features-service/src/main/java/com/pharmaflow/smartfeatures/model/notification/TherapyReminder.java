package com.pharmaflow.smartfeatures.model.notification;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
 * Represents a therapy reminder for a patient profile and product.
 */
@Entity
@Table(name = "therapy_reminder")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TherapyReminder {

    /** Primary key of the reminder. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id", nullable = false, updatable = false)
    private Long reminderId;

    /** External reference to the patient profile. */
    @Column(name = "patient_profile_id", nullable = false)
    private Long patientProfileId;

    /** External reference to the product. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** Dosage instructions shown to the user. */
    @Column(name = "dosage_instruction")
    private String dosageInstruction;

    /** Number of reminder occurrences planned per day. */
    @Column(name = "frequency_per_day")
    private Integer frequencyPerDay;

    /** Date when the reminder schedule starts. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Optional date when the reminder schedule ends. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Timestamp of the next scheduled reminder. */
    @Column(name = "next_reminder_at")
    private LocalDateTime nextReminderAt;

    /** Current reminder state. */
    @Column(name = "status", nullable = false)
    private String status;

    /** Notifications triggered from this reminder. */
    @Default
    @OneToMany(mappedBy = "therapyReminder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();
}

package com.pharmaflow.smartfeatures.model.symptom;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Represents a symptom search action performed by a user.
 */
@Entity
@Table(name = "symptom_search")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomSearch {

    /** Primary key of the search record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id", nullable = false, updatable = false)
    private Long searchId;

    /** External reference to the user who performed the search. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Optional external reference to the patient profile. */
    @Column(name = "patient_profile_id")
    private Long patientProfileId;

    /** Raw search input entered by the user. */
    @Column(name = "search_query", nullable = false)
    private String searchQuery;

    /** Timestamp when the search was performed. */
    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    /** Symptoms linked to this search action. */
    @Default
    @OneToMany(mappedBy = "search", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SymptomSearchItem> items = new ArrayList<>();
}

package com.pharmaflow.smartfeatures.model.symptom;

import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
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
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a normalized symptom used by search and recommendation flows.
 */
@Entity
@Table(name = "symptom")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Symptom {

    /** Primary key of the symptom record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "symptom_id", nullable = false, updatable = false)
    private Long symptomId;

    /** Display name of the symptom. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Optional description that clarifies the symptom. */
    @Column(name = "description")
    private String description;

    /** Optional severity level for the symptom. */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level")
    private SymptomSeverityLevel severityLevel;

    /** Indicates whether the symptom is currently available for use. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** Search items that reference this symptom. */
    @Default
    @OneToMany(mappedBy = "symptom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SymptomSearchItem> searchItems = new ArrayList<>();

    /** Product matches associated with this symptom. */
    @Default
    @OneToMany(mappedBy = "symptom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SymptomProductMatch> productMatches = new ArrayList<>();
}

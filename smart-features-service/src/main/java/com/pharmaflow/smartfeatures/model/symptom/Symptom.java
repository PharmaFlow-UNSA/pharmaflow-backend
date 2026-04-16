package com.pharmaflow.smartfeatures.model.symptom;

import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.util.SymptomTextNormalizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
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
 * Represents a normalized symptom used by search and recommendation flows.
 */
@Entity
@Table(
        name = "symptom",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_symptom_normalized_name", columnNames = "normalized_name")
        })
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
    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Normalized name used for consistent duplicate checks. */
    @NotBlank
    @Size(max = 100)
    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    /** Optional description that clarifies the symptom. */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /** Optional severity level for the symptom. */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", length = 20)
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

    @PrePersist
    @PreUpdate
    void syncNormalizedName() {
        name = SymptomTextNormalizer.sanitizeName(name);
        description = SymptomTextNormalizer.sanitizeDescription(description);
        normalizedName = SymptomTextNormalizer.normalizeName(name);
    }
}

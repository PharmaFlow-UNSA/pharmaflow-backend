package com.pharmaflow.producthealth.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contraindications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contraindication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Supstanca za koju vrijedi kontraindikacija
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substance_id", nullable = false)
    private Substance substance;

    // Tip kontraindikacije (dijagnoza, dob, stanje...)
    @Enumerated(EnumType.STRING)
    @Column(name = "contraindication_type", nullable = false)
    private ContraindicationType type;

    // Condition name (e.g. "Renal insufficiency", "Pregnancy")
    @NotBlank
    @Column(nullable = false)
    private String conditionName;

    // Detailed description of why it is contraindicated
    @Column(columnDefinition = "TEXT")
    private String description;

    // Ozbiljnost: da li je apsolutna ili relativna kontraindikacija
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityType severityType;

    public enum ContraindicationType {
        DISEASE,        // Chronic disease (e.g. liver insufficiency)
        ALLERGY,        // Alergija na supstancu
        AGE,            // Dob (npr. djeca ispod 12 godina)
        PREGNANCY,      // Pregnancy
        BREASTFEEDING,  // Dojenje
        CONDITION       // Ostala stanja (npr. G6PD deficit)
    }

    public enum SeverityType {
        ABSOLUTE,   // Apsolutna kontraindikacija - nikako ne koristiti
        RELATIVE    // Relativna - koristiti s oprezom uz nadzor ljekara
    }
}
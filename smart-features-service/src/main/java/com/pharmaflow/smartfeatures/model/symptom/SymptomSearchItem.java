package com.pharmaflow.smartfeatures.model.symptom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join entity between a symptom search and the symptoms selected in it.
 */
@Entity
@Table(name = "symptom_search_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomSearchItem {

    /** Primary key of the join record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_item_id", nullable = false, updatable = false)
    private Long searchItemId;

    /** Parent search that contains this item. */
    @ManyToOne
    @JoinColumn(name = "search_id", nullable = false)
    private SymptomSearch search;

    /** Symptom linked to the search. */
    @ManyToOne
    @JoinColumn(name = "symptom_id", nullable = false)
    private Symptom symptom;
}

package com.pharmaflow.smartfeatures.model.chat;

import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
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
 * Represents an FAQ knowledge base entry used for chat assistance.
 */
@Entity
@Table(name = "faq_entry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqEntry {

    /** Primary key of the FAQ entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id", nullable = false, updatable = false)
    private Long faqId;

    /** Question shown in the FAQ knowledge base. */
    @Column(name = "question", nullable = false)
    private String question;

    /** Answer text associated with the question. */
    @Column(name = "answer", nullable = false)
    private String answer;

    /** Functional category used to group FAQ entries. */
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private FaqCategory category;

    /** Keywords that help match a user message to this entry. */
    @Column(name = "keywords")
    private String keywords;

    /** Indicates whether the FAQ entry can currently be matched. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** Timestamp of the last update to the FAQ entry. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Intent matches that resolved to this FAQ entry. */
    @Default
    @OneToMany(mappedBy = "faqEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatIntentMatch> intentMatches = new ArrayList<>();
}

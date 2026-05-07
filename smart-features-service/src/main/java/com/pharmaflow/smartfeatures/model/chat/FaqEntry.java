package com.pharmaflow.smartfeatures.model.chat;

import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents an FAQ knowledge base entry used for chat assistance. */
@Entity
@Table(
    name = "faq_entry",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_faq_entry_normalized_question",
          columnNames = "normalized_question")
    })
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
  @NotBlank
  @Size(max = 200)
  @Column(name = "question", nullable = false, length = 200)
  private String question;

  /** Normalized question used for duplicate detection. */
  @NotBlank
  @Size(max = 200)
  @Column(name = "normalized_question", nullable = false, length = 200)
  private String normalizedQuestion;

  /** Answer text associated with the question. */
  @NotBlank
  @Size(max = 2000)
  @Column(name = "answer", nullable = false, length = 2000)
  private String answer;

  /** Functional category used to group FAQ entries. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "category")
  private FaqCategory category;

  /** Keywords that help match a user message to this entry. */
  @Size(max = 500)
  @Column(name = "keywords", length = 500)
  private String keywords;

  /** Indicates whether the FAQ entry can currently be matched. */
  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  /** Timestamp of the last update to the FAQ entry. */
  @NotNull
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** Intent matches that resolved to this FAQ entry. */
  @Default
  @OneToMany(mappedBy = "faqEntry", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ChatIntentMatch> intentMatches = new ArrayList<>();

  @PrePersist
  @PreUpdate
  void normalizeFields() {
    question = TextSanitizer.sanitizeRequiredText(question);
    answer = TextSanitizer.sanitizeRequiredText(answer);
    keywords = TextSanitizer.sanitizeOptionalText(keywords);
    normalizedQuestion = TextSanitizer.normalizeKey(question);
  }
}

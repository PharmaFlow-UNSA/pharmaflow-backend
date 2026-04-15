package com.pharmaflow.smartfeatures.model.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the interpretation of a chat message against FAQ knowledge.
 */
@Entity
@Table(name = "chat_intent_match")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatIntentMatch {

    /** Primary key of the intent match record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "intent_match_id", nullable = false, updatable = false)
    private Long intentMatchId;

    /** Source message that was analyzed. */
    @NotNull
    @OneToOne
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private ChatMessage message;

    /** FAQ entry that best matched the detected intent. */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "faq_id", nullable = false)
    private FaqEntry faqEntry;

    /** Name of the intent detected from the message. */
    @NotBlank
    @Size(max = 100)
    @Column(name = "detected_intent", nullable = false, length = 100)
    private String detectedIntent;

    /** Confidence score for the detected intent. */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "confidence_score")
    private Double confidenceScore;
}

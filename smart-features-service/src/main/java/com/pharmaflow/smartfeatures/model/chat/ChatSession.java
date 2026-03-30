package com.pharmaflow.smartfeatures.model.chat;

import com.pharmaflow.smartfeatures.enums.chat.ChatSessionStatus;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
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
 * Represents a chat conversation in the smart features domain.
 */
@Entity
@Table(name = "chat_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /** Primary key of the chat session. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    /** External reference to the user. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Optional external reference to the patient profile. */
    @Column(name = "patient_profile_id")
    private Long patientProfileId;

    /** Type of chat flow used for the session. */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private ChatSessionType sessionType;

    /** Current state of the session. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChatSessionStatus status;

    /** Timestamp when the session started. */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Optional timestamp when the session ended. */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** Messages exchanged within the session. */
    @Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();
}

package com.pharmaflow.smartfeatures.model.chat;

import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents an individual message sent inside a chat session. */
@Entity
@Table(name = "chat_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

  /** Primary key of the message. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "message_id", nullable = false, updatable = false)
  private Long messageId;

  /** Parent chat session that contains this message. */
  @NotNull
  @ManyToOne
  @JoinColumn(name = "session_id", nullable = false)
  private ChatSession session;

  /** Role of the sender. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "sender_type", nullable = false)
  private ChatSenderType senderType;

  /** Optional external reference to the sender. */
  @Positive
  @Column(name = "sender_id")
  private Long senderId;

  /** Main text content of the message. */
  @Size(max = 2000)
  @Column(name = "message_text", length = 2000)
  private String messageText;

  /** Optional URL for an attachment linked to the message. */
  @Size(max = 1000)
  @Column(name = "attachment_url", length = 1000)
  private String attachmentUrl;

  /** Timestamp when the message was created. */
  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** Optional intent analysis linked to the message. */
  @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
  private ChatIntentMatch intentMatch;
}

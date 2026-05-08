package com.pharmaflow.smartfeatures.service.chat;

import com.pharmaflow.smartfeatures.dto.chat.ChatIntentMatchResponseDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageResponseDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatSessionRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatSessionResponseDto;
import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionStatus;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.chat.ChatMapper;
import com.pharmaflow.smartfeatures.model.chat.ChatIntentMatch;
import com.pharmaflow.smartfeatures.model.chat.ChatMessage;
import com.pharmaflow.smartfeatures.model.chat.ChatSession;
import com.pharmaflow.smartfeatures.model.chat.FaqEntry;
import com.pharmaflow.smartfeatures.repositories.chat.ChatIntentMatchRepository;
import com.pharmaflow.smartfeatures.repositories.chat.ChatMessageRepository;
import com.pharmaflow.smartfeatures.repositories.chat.ChatSessionRepository;
import com.pharmaflow.smartfeatures.repositories.chat.FaqEntryRepository;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

  private final ChatSessionRepository chatSessionRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatIntentMatchRepository chatIntentMatchRepository;
  private final FaqEntryRepository faqEntryRepository;
  private final ChatMapper chatMapper;

  public ChatService(
      ChatSessionRepository chatSessionRepository,
      ChatMessageRepository chatMessageRepository,
      ChatIntentMatchRepository chatIntentMatchRepository,
      FaqEntryRepository faqEntryRepository,
      ChatMapper chatMapper) {
    this.chatSessionRepository = chatSessionRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.chatIntentMatchRepository = chatIntentMatchRepository;
    this.faqEntryRepository = faqEntryRepository;
    this.chatMapper = chatMapper;
  }

  @Transactional
  public ChatSessionResponseDto createSession(ChatSessionRequestDto requestDto) {
    ChatSession session = chatMapper.toEntity(requestDto);
    session.setStatus(ChatSessionStatus.OPEN);
    session.setStartedAt(LocalDateTime.now());
    session.setEndedAt(null);
    return chatMapper.toResponseDto(chatSessionRepository.save(session));
  }

  @Transactional(readOnly = true)
  public ChatSessionResponseDto getSession(Long id) {
    return chatMapper.toResponseDto(findSessionById(id));
  }

  @Transactional(readOnly = true)
  public List<ChatSessionResponseDto> getSessionsByUser(Long userId) {
    return chatSessionRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
        .map(chatMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public ChatSessionResponseDto closeSession(Long id) {
    ChatSession session = findSessionById(id);
    session.setStatus(ChatSessionStatus.CLOSED);
    if (session.getEndedAt() == null) {
      session.setEndedAt(LocalDateTime.now());
    }
    return chatMapper.toResponseDto(chatSessionRepository.save(session));
  }

  @Transactional
  public ChatMessageResponseDto createMessage(Long sessionId, ChatMessageRequestDto requestDto) {
    ChatSession session = findSessionById(sessionId);
    if (session.getStatus() == ChatSessionStatus.CLOSED) {
      throw new BadRequestException("Cannot add a message to a closed chat session.");
    }

    validateSender(requestDto);
    ChatMessage message = chatMapper.toEntity(requestDto);
    message.setSession(session);
    message.setMessageText(TextSanitizer.sanitizeOptionalText(requestDto.getMessageText()));
    message.setAttachmentUrl(
        requestDto.getAttachmentUrl() != null ? requestDto.getAttachmentUrl().trim() : null);
    message.setCreatedAt(LocalDateTime.now());

    ChatMessage savedMessage = chatMessageRepository.save(message);
    maybeCreateIntentMatch(savedMessage, session);
    return chatMapper.toResponseDto(savedMessage);
  }

  @Transactional(readOnly = true)
  public List<ChatMessageResponseDto> getMessages(Long sessionId) {
    findSessionById(sessionId);
    return chatMessageRepository.findBySessionSessionIdOrderByCreatedAtAsc(sessionId).stream()
        .map(chatMapper::toResponseDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public ChatIntentMatchResponseDto getIntentMatch(Long messageId) {
    ChatIntentMatch intentMatch =
        chatIntentMatchRepository
            .findByMessageMessageId(messageId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Chat intent match not found for message id: " + messageId));
    return chatMapper.toResponseDto(intentMatch);
  }

  private ChatSession findSessionById(Long id) {
    return chatSessionRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Chat session not found with id: " + id));
  }

  private void validateSender(ChatMessageRequestDto requestDto) {
    if ((requestDto.getSenderType() == ChatSenderType.USER
            || requestDto.getSenderType() == ChatSenderType.PHARMACIST)
        && requestDto.getSenderId() == null) {
      throw new BadRequestException("senderId is required for USER and PHARMACIST senders.");
    }

    if ((requestDto.getSenderType() == ChatSenderType.BOT
            || requestDto.getSenderType() == ChatSenderType.SYSTEM)
        && requestDto.getSenderId() != null) {
      throw new BadRequestException("senderId must not be provided for BOT or SYSTEM senders.");
    }
  }

  private void maybeCreateIntentMatch(ChatMessage message, ChatSession session) {
    String sanitizedMessageText = TextSanitizer.sanitizeOptionalText(message.getMessageText());
    if (session.getSessionType() != ChatSessionType.FAQ_BOT
        || message.getSenderType() != ChatSenderType.USER
        || sanitizedMessageText == null) {
      return;
    }

    List<FaqEntry> matches = faqEntryRepository.searchActive(sanitizedMessageText);
    if (matches.isEmpty()) {
      return;
    }

    FaqEntry bestMatch =
        matches.stream()
            .max(
                Comparator.comparingDouble(
                    faqEntry -> scoreFaqMatch(faqEntry, sanitizedMessageText)))
            .orElse(null);

    double confidence = scoreFaqMatch(bestMatch, sanitizedMessageText);
    ChatIntentMatch intentMatch =
        ChatIntentMatch.builder()
            .message(message)
            .faqEntry(bestMatch)
            .detectedIntent(bestMatch.getCategory().name())
            .confidenceScore(confidence)
            .build();
    chatIntentMatchRepository.save(intentMatch);
  }

  private double scoreFaqMatch(FaqEntry faqEntry, String sanitizedMessageText) {
    String normalizedQuery = normalize(sanitizedMessageText);
    if (normalizedQuery == null) {
      return 0.0;
    }

    String normalizedQuestion = normalize(faqEntry.getQuestion());
    if (normalizedQuery.equals(normalizedQuestion)) {
      return 1.0;
    }

    double questionScore = fieldMatchScore(normalizedQuestion, normalizedQuery);
    double keywordScore = fieldMatchScore(normalize(faqEntry.getKeywords()), normalizedQuery);
    double answerScore = fieldMatchScore(normalize(faqEntry.getAnswer()), normalizedQuery);

    return clamp((questionScore * 0.55) + (keywordScore * 0.30) + (answerScore * 0.15));
  }

  private double fieldMatchScore(String fieldText, String normalizedQuery) {
    if (fieldText == null) {
      return 0.0;
    }

    double phraseScore = fieldText.contains(normalizedQuery) ? 1.0 : 0.0;
    Set<String> queryTokens = tokens(normalizedQuery);
    Set<String> fieldTokens = tokens(fieldText);
    if (queryTokens.isEmpty() || fieldTokens.isEmpty()) {
      return phraseScore;
    }

    long matchingTokens = queryTokens.stream().filter(fieldTokens::contains).count();
    double tokenScore = (double) matchingTokens / queryTokens.size();
    return Math.max(phraseScore, tokenScore);
  }

  private Set<String> tokens(String value) {
    return Arrays.stream(value.split("[^\\p{L}\\p{N}]+"))
        .map(String::trim)
        .filter(token -> token.length() >= 2)
        .collect(LinkedHashSet::new, Set::add, Set::addAll);
  }

  private String normalize(String value) {
    String sanitized = TextSanitizer.sanitizeOptionalText(value);
    return sanitized == null ? null : sanitized.toLowerCase(Locale.ROOT);
  }

  private double clamp(double score) {
    return Math.max(0.0, Math.min(1.0, score));
  }
}

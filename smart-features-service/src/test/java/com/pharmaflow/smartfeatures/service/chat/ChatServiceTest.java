package com.pharmaflow.smartfeatures.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageResponseDto;
import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionStatus;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.mapper.chat.ChatMapper;
import com.pharmaflow.smartfeatures.model.chat.ChatIntentMatch;
import com.pharmaflow.smartfeatures.model.chat.ChatMessage;
import com.pharmaflow.smartfeatures.model.chat.ChatSession;
import com.pharmaflow.smartfeatures.model.chat.FaqEntry;
import com.pharmaflow.smartfeatures.repositories.chat.ChatIntentMatchRepository;
import com.pharmaflow.smartfeatures.repositories.chat.ChatMessageRepository;
import com.pharmaflow.smartfeatures.repositories.chat.ChatSessionRepository;
import com.pharmaflow.smartfeatures.repositories.chat.FaqEntryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatIntentMatchRepository chatIntentMatchRepository;

    @Mock
    private FaqEntryRepository faqEntryRepository;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                chatSessionRepository,
                chatMessageRepository,
                chatIntentMatchRepository,
                faqEntryRepository,
                new ChatMapper(new ModelMapperConfig().modelMapper()));
    }

    @Test
    void createMessageShouldRejectClosedSession() {
        ChatSession closedSession = ChatSession.builder()
                .sessionId(1L)
                .status(ChatSessionStatus.CLOSED)
                .sessionType(ChatSessionType.FAQ_BOT)
                .userId(10L)
                .build();
        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(closedSession));

        assertThatThrownBy(() -> chatService.createMessage(
                        1L, new ChatMessageRequestDto(ChatSenderType.USER, 10L, "Need help", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot add a message to a closed chat session.");

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void createMessageShouldCreateIntentMatchForBestFaqCandidate() {
        ChatSession session = ChatSession.builder()
                .sessionId(2L)
                .status(ChatSessionStatus.OPEN)
                .sessionType(ChatSessionType.FAQ_BOT)
                .userId(10L)
                .build();
        FaqEntry faqEntry = FaqEntry.builder()
                .faqId(5L)
                .question("How to pay?")
                .answer("Use card or cash.")
                .keywords("payment,pay")
                .category(FaqCategory.PAYMENTS)
                .isActive(true)
                .build();

        when(chatSessionRepository.findById(2L)).thenReturn(Optional.of(session));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setMessageId(9L);
            return message;
        });
        when(faqEntryRepository.searchActive("How to pay?")).thenReturn(List.of(faqEntry));
        when(chatIntentMatchRepository.save(any(ChatIntentMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageResponseDto response = chatService.createMessage(
                2L,
                new ChatMessageRequestDto(
                        ChatSenderType.USER, 10L, "  How to pay?  ", "  https://example.com/info  "));

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getMessageText()).isEqualTo("How to pay?");
        assertThat(messageCaptor.getValue().getAttachmentUrl()).isEqualTo("https://example.com/info");

        ArgumentCaptor<ChatIntentMatch> intentCaptor = ArgumentCaptor.forClass(ChatIntentMatch.class);
        verify(chatIntentMatchRepository).save(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getDetectedIntent()).isEqualTo("PAYMENTS");
        assertThat(intentCaptor.getValue().getFaqEntry().getFaqId()).isEqualTo(5L);
        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getSessionId()).isEqualTo(2L);
    }

    @Test
    void createMessageShouldRejectBotWithSenderId() {
        ChatSession session = ChatSession.builder()
                .sessionId(3L)
                .status(ChatSessionStatus.OPEN)
                .sessionType(ChatSessionType.PHARMACIST_CHAT)
                .userId(10L)
                .build();
        when(chatSessionRepository.findById(3L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> chatService.createMessage(
                        3L, new ChatMessageRequestDto(ChatSenderType.BOT, 22L, "Automated", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("senderId must not be provided for BOT or SYSTEM senders.");

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
}

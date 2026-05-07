package com.pharmaflow.smartfeatures.controller.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.chat.ChatSessionResponseDto;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionStatus;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.service.chat.ChatService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ChatService chatService;

  private ChatSessionResponseDto sessionResponseDto;

  @BeforeEach
  void setUp() {
    sessionResponseDto = new ChatSessionResponseDto();
    sessionResponseDto.setId(1L);
    sessionResponseDto.setUserId(10L);
    sessionResponseDto.setPatientProfileId(20L);
    sessionResponseDto.setSessionType(ChatSessionType.FAQ_BOT);
    sessionResponseDto.setStatus(ChatSessionStatus.OPEN);
    sessionResponseDto.setStartedAt(LocalDateTime.now());
  }

  @Test
  void createSessionShouldReturn201() throws Exception {
    when(chatService.createSession(any())).thenReturn(sessionResponseDto);

    mockMvc
        .perform(
            post("/api/chat-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userId": 10,
                                  "patientProfileId": 20,
                                  "sessionType": "FAQ_BOT"
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void createMessageShouldRejectBodyWithoutMessageOrAttachment() throws Exception {
    mockMvc
        .perform(
            post("/api/chat-sessions/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "senderType": "USER",
                                  "senderId": 10
                                }
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(chatService, never()).createMessage(any(), any());
  }
}

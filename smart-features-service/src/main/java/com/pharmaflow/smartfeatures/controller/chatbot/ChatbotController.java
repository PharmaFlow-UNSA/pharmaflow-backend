package com.pharmaflow.smartfeatures.controller.chatbot;

import com.pharmaflow.smartfeatures.dto.chatbot.ChatbotAskRequestDto;
import com.pharmaflow.smartfeatures.dto.chatbot.ChatbotAskResponseDto;
import com.pharmaflow.smartfeatures.service.chatbot.ChatbotService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot")
public class ChatbotController {

  private final ChatbotService chatbotService;

  public ChatbotController(ChatbotService chatbotService) {
    this.chatbotService = chatbotService;
  }

  @PostMapping("/ask")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<ChatbotAskResponseDto> ask(
      @Valid @RequestBody ChatbotAskRequestDto requestDto) {
    return ResponseEntity.ok(chatbotService.ask(requestDto));
  }
}

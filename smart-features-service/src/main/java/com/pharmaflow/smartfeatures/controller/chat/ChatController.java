package com.pharmaflow.smartfeatures.controller.chat;

import com.pharmaflow.smartfeatures.dto.chat.ChatIntentMatchResponseDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageResponseDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatSessionRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatSessionResponseDto;
import com.pharmaflow.smartfeatures.service.chat.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat-sessions")
    public ResponseEntity<ChatSessionResponseDto> createSession(@Valid @RequestBody ChatSessionRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createSession(requestDto));
    }

    @GetMapping("/api/chat-sessions/{id}")
    public ResponseEntity<ChatSessionResponseDto> getSession(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(chatService.getSession(id));
    }

    @GetMapping("/api/chat-sessions/user/{userId}")
    public ResponseEntity<List<ChatSessionResponseDto>> getSessionsByUser(@PathVariable @Positive Long userId) {
        return ResponseEntity.ok(chatService.getSessionsByUser(userId));
    }

    @PatchMapping("/api/chat-sessions/{id}/close")
    public ResponseEntity<ChatSessionResponseDto> closeSession(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(chatService.closeSession(id));
    }

    @PostMapping("/api/chat-sessions/{id}/messages")
    public ResponseEntity<ChatMessageResponseDto> createMessage(
            @PathVariable @Positive Long id, @Valid @RequestBody ChatMessageRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createMessage(id, requestDto));
    }

    @GetMapping("/api/chat-sessions/{id}/messages")
    public ResponseEntity<List<ChatMessageResponseDto>> getMessages(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }

    @GetMapping("/api/chat-messages/{messageId}/intent-match")
    public ResponseEntity<ChatIntentMatchResponseDto> getIntentMatch(@PathVariable @Positive Long messageId) {
        return ResponseEntity.ok(chatService.getIntentMatch(messageId));
    }
}

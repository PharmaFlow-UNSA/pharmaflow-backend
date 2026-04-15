package com.pharmaflow.smartfeatures.dto.chat;

import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDto {

    private Long id;
    private Long sessionId;
    private ChatSenderType senderType;
    private Long senderId;
    private String messageText;
    private String attachmentUrl;
    private LocalDateTime createdAt;
}

package com.pharmaflow.smartfeatures.dto.chat;

import com.pharmaflow.smartfeatures.enums.chat.ChatSessionStatus;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponseDto {

  private Long id;
  private Long userId;
  private Long patientProfileId;
  private ChatSessionType sessionType;
  private ChatSessionStatus status;
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;
}

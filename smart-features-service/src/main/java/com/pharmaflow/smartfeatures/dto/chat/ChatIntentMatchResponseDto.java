package com.pharmaflow.smartfeatures.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatIntentMatchResponseDto {

  private Long id;
  private Long messageId;
  private Long faqId;
  private String detectedIntent;
  private Double confidenceScore;
}

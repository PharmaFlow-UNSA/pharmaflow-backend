package com.pharmaflow.smartfeatures.dto.chatbot;

import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotAskResponseDto {

  private String answer;
  private String matchedQuestion;
  private Double confidence;
  private FaqCategory category;
  private Boolean fallback;
}

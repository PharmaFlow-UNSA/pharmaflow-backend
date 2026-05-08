package com.pharmaflow.smartfeatures.dto.chatbot;

import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotAskRequestDto {

  @SanitizedTextSize(
      min = 2,
      max = 2000,
      allowBlank = false,
      message = "message must be between 2 and 2000 characters after trimming")
  private String message;
}

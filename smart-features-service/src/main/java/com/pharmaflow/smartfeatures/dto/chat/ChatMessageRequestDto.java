package com.pharmaflow.smartfeatures.dto.chat;

import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import com.pharmaflow.smartfeatures.validation.AtLeastOneOf;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AtLeastOneOf(
    fields = {"messageText", "attachmentUrl"},
    message = "Either messageText or attachmentUrl must be provided")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDto {

  @NotNull(message = "senderType is required")
  private ChatSenderType senderType;

  @NullablePositive(message = "senderId must be positive")
  private Long senderId;

  @SanitizedTextSize(max = 2000, message = "messageText must not exceed 2000 characters")
  private String messageText;

  @Pattern(
      regexp = "^(https?://\\S+)?$",
      message = "attachmentUrl must be a valid http or https URL")
  private String attachmentUrl;
}

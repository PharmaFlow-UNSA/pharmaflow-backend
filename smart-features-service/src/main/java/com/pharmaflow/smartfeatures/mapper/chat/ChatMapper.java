package com.pharmaflow.smartfeatures.mapper.chat;

import com.pharmaflow.smartfeatures.dto.chat.ChatIntentMatchResponseDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageResponseDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatSessionRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatSessionResponseDto;
import com.pharmaflow.smartfeatures.model.chat.ChatIntentMatch;
import com.pharmaflow.smartfeatures.model.chat.ChatMessage;
import com.pharmaflow.smartfeatures.model.chat.ChatSession;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

  private final ModelMapper modelMapper;

  public ChatMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  public ChatSession toEntity(ChatSessionRequestDto requestDto) {
    return modelMapper.map(requestDto, ChatSession.class);
  }

  public ChatMessage toEntity(ChatMessageRequestDto requestDto) {
    return modelMapper.map(requestDto, ChatMessage.class);
  }

  public ChatSessionResponseDto toResponseDto(ChatSession session) {
    ChatSessionResponseDto response = modelMapper.map(session, ChatSessionResponseDto.class);
    response.setId(session.getSessionId());
    return response;
  }

  public ChatMessageResponseDto toResponseDto(ChatMessage message) {
    ChatMessageResponseDto response = modelMapper.map(message, ChatMessageResponseDto.class);
    response.setId(message.getMessageId());
    response.setSessionId(message.getSession().getSessionId());
    return response;
  }

  public ChatIntentMatchResponseDto toResponseDto(ChatIntentMatch intentMatch) {
    ChatIntentMatchResponseDto response =
        modelMapper.map(intentMatch, ChatIntentMatchResponseDto.class);
    response.setId(intentMatch.getIntentMatchId());
    response.setMessageId(intentMatch.getMessage().getMessageId());
    response.setFaqId(intentMatch.getFaqEntry().getFaqId());
    return response;
  }
}

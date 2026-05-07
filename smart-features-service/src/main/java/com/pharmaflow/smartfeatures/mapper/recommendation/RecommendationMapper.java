package com.pharmaflow.smartfeatures.mapper.recommendation;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationEventResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.model.recommendation.RecommendationEvent;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RecommendationMapper {

  private final ModelMapper modelMapper;

  public RecommendationMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  public Recommendation toEntity(RecommendationRequestDto requestDto) {
    return modelMapper.map(requestDto, Recommendation.class);
  }

  public void updateEntity(RecommendationRequestDto requestDto, Recommendation recommendation) {
    modelMapper.map(requestDto, recommendation);
  }

  public RecommendationResponseDto toResponseDto(Recommendation recommendation) {
    RecommendationResponseDto response =
        modelMapper.map(recommendation, RecommendationResponseDto.class);
    response.setId(recommendation.getRecommendationId());
    return response;
  }

  public RecommendationEventResponseDto toEventResponseDto(RecommendationEvent event) {
    RecommendationEventResponseDto response =
        modelMapper.map(event, RecommendationEventResponseDto.class);
    response.setId(event.getEventId());
    response.setRecommendationId(event.getRecommendation().getRecommendationId());
    return response;
  }
}

package com.pharmaflow.smartfeatures.mapper.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SymptomProductMatchMapper {

  private final ModelMapper modelMapper;

  public SymptomProductMatchMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  public SymptomProductMatch toEntity(SymptomProductMatchRequestDto requestDto) {
    return modelMapper.map(requestDto, SymptomProductMatch.class);
  }

  public void updateEntity(SymptomProductMatchRequestDto requestDto, SymptomProductMatch match) {
    modelMapper.map(requestDto, match);
  }

  public SymptomProductMatchResponseDto toResponseDto(SymptomProductMatch match) {
    SymptomProductMatchResponseDto response =
        modelMapper.map(match, SymptomProductMatchResponseDto.class);
    response.setId(match.getMatchId());
    response.setSymptomId(match.getSymptom().getSymptomId());
    response.setMatchedSymptomIds(List.of(match.getSymptom().getSymptomId()));
    return response;
  }
}

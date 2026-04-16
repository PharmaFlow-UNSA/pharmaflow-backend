package com.pharmaflow.smartfeatures.mapper.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomResponseDto;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SymptomMapper {

    private final ModelMapper modelMapper;

    public SymptomMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Symptom toEntity(SymptomRequestDto requestDto) {
        return modelMapper.map(requestDto, Symptom.class);
    }

    public SymptomResponseDto toResponseDto(Symptom symptom) {
        return modelMapper.map(symptom, SymptomResponseDto.class);
    }

    public void updateEntity(SymptomRequestDto requestDto, Symptom symptom) {
        modelMapper.map(requestDto, symptom);
    }
}

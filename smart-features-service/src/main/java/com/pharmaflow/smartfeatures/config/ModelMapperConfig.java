package com.pharmaflow.smartfeatures.config;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomResponseDto;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

  @Bean
  public ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper
        .getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STRICT)
        .setFieldMatchingEnabled(true)
        .setAmbiguityIgnored(false)
        .setSkipNullEnabled(false);

    modelMapper
        .typeMap(Symptom.class, SymptomResponseDto.class)
        .addMappings(
            mapper -> {
              mapper.map(Symptom::getSymptomId, SymptomResponseDto::setId);
              mapper.map(Symptom::isActive, SymptomResponseDto::setActive);
            });

    modelMapper
        .typeMap(SymptomRequestDto.class, Symptom.class)
        .addMappings(mapper -> mapper.map(SymptomRequestDto::getActive, Symptom::setActive));

    return modelMapper;
  }
}

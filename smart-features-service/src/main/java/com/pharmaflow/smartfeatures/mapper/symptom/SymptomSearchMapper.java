package com.pharmaflow.smartfeatures.mapper.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemResponseDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchResponseDto;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearchItem;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SymptomSearchMapper {

    private final ModelMapper modelMapper;

    public SymptomSearchMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public SymptomSearchResponseDto toResponseDto(SymptomSearch search) {
        SymptomSearchResponseDto response = modelMapper.map(search, SymptomSearchResponseDto.class);
        response.setId(search.getSearchId());
        return response;
    }

    public SymptomSearchItemResponseDto toItemResponseDto(SymptomSearchItem item) {
        SymptomSearchItemResponseDto response = modelMapper.map(item, SymptomSearchItemResponseDto.class);
        response.setId(item.getSearchItemId());
        response.setSearchId(item.getSearch().getSearchId());
        response.setSymptomId(item.getSymptom().getSymptomId());
        response.setSymptomName(item.getSymptom().getName());
        return response;
    }
}

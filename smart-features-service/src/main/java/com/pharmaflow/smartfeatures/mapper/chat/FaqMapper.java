package com.pharmaflow.smartfeatures.mapper.chat;

import com.pharmaflow.smartfeatures.dto.chat.FaqEntryRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryResponseDto;
import com.pharmaflow.smartfeatures.model.chat.FaqEntry;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class FaqMapper {

    private final ModelMapper modelMapper;

    public FaqMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public FaqEntry toEntity(FaqEntryRequestDto requestDto) {
        FaqEntry entity = modelMapper.map(requestDto, FaqEntry.class);
        entity.setActive(Boolean.TRUE.equals(requestDto.getActive()));
        return entity;
    }

    public void updateEntity(FaqEntryRequestDto requestDto, FaqEntry faqEntry) {
        modelMapper.map(requestDto, faqEntry);
        faqEntry.setActive(Boolean.TRUE.equals(requestDto.getActive()));
    }

    public FaqEntryResponseDto toResponseDto(FaqEntry faqEntry) {
        FaqEntryResponseDto response = modelMapper.map(faqEntry, FaqEntryResponseDto.class);
        response.setId(faqEntry.getFaqId());
        response.setActive(faqEntry.isActive());
        return response;
    }
}

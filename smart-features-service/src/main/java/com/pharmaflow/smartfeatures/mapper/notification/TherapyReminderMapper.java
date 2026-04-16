package com.pharmaflow.smartfeatures.mapper.notification;

import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderResponseDto;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class TherapyReminderMapper {

    private final ModelMapper modelMapper;

    public TherapyReminderMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public TherapyReminder toEntity(TherapyReminderRequestDto requestDto) {
        return modelMapper.map(requestDto, TherapyReminder.class);
    }

    public void updateEntity(TherapyReminderRequestDto requestDto, TherapyReminder reminder) {
        modelMapper.map(requestDto, reminder);
    }

    public TherapyReminderResponseDto toResponseDto(TherapyReminder reminder) {
        TherapyReminderResponseDto response = modelMapper.map(reminder, TherapyReminderResponseDto.class);
        response.setId(reminder.getReminderId());
        return response;
    }
}

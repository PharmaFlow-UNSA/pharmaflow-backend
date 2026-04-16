package com.pharmaflow.smartfeatures.mapper.notification;

import com.pharmaflow.smartfeatures.dto.notification.NotificationRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationResponseDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationTriggerResponseDto;
import com.pharmaflow.smartfeatures.model.notification.Notification;
import com.pharmaflow.smartfeatures.model.notification.NotificationTrigger;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    private final ModelMapper modelMapper;

    public NotificationMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Notification toEntity(NotificationRequestDto requestDto) {
        return modelMapper.map(requestDto, Notification.class);
    }

    public void updateEntity(NotificationRequestDto requestDto, Notification notification) {
        modelMapper.map(requestDto, notification);
    }

    public NotificationResponseDto toResponseDto(Notification notification) {
        NotificationResponseDto response = modelMapper.map(notification, NotificationResponseDto.class);
        response.setId(notification.getNotificationId());
        response.setTherapyReminderId(
                notification.getTherapyReminder() != null
                        ? notification.getTherapyReminder().getReminderId()
                        : null);
        return response;
    }

    public NotificationTriggerResponseDto toTriggerResponseDto(NotificationTrigger trigger) {
        NotificationTriggerResponseDto response = modelMapper.map(trigger, NotificationTriggerResponseDto.class);
        response.setId(trigger.getTriggerId());
        response.setNotificationId(trigger.getNotification().getNotificationId());
        return response;
    }
}

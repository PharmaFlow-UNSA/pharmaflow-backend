package com.pharmaflow.smartfeatures.controller.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.notification.NotificationResponseDto;
import com.pharmaflow.smartfeatures.enums.notification.NotificationChannel;
import com.pharmaflow.smartfeatures.enums.notification.NotificationStatus;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.service.notification.NotificationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private NotificationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        responseDto = new NotificationResponseDto();
        responseDto.setId(1L);
        responseDto.setTherapyReminderId(7L);
        responseDto.setUserId(10L);
        responseDto.setPatientProfileId(20L);
        responseDto.setType(NotificationType.THERAPY_REMINDER);
        responseDto.setChannel(NotificationChannel.IN_APP);
        responseDto.setTitle("Reminder");
        responseDto.setMessage("Time for therapy");
        responseDto.setStatus(NotificationStatus.PENDING);
        responseDto.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createNotificationShouldReturn201() throws Exception {
        when(notificationService.createNotification(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapyReminderId": 7,
                                  "userId": 10,
                                  "patientProfileId": 20,
                                  "type": "THERAPY_REMINDER",
                                  "channel": "IN_APP",
                                  "title": "Reminder",
                                  "message": "Time for therapy"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void updateDeliveryStatusShouldTranslateServiceBadRequest() throws Exception {
        when(notificationService.updateDeliveryStatus(any(), any()))
                .thenThrow(new BadRequestException("Invalid notification status transition from PENDING to DELIVERED."));

        mockMvc.perform(patch("/api/notifications/1/delivery-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DELIVERED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid notification status transition from PENDING to DELIVERED."));
    }
}

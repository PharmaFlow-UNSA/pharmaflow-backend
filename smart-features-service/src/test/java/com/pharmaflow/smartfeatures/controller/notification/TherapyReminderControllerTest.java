package com.pharmaflow.smartfeatures.controller.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderResponseDto;
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.service.notification.TherapyReminderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TherapyReminderController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TherapyReminderControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private TherapyReminderService therapyReminderService;

  private TherapyReminderResponseDto responseDto;

  @BeforeEach
  void setUp() {
    responseDto = new TherapyReminderResponseDto();
    responseDto.setId(1L);
    responseDto.setPatientProfileId(10L);
    responseDto.setProductId(20L);
    responseDto.setDosageInstruction("Take once daily");
    responseDto.setFrequencyPerDay(1);
    responseDto.setStartDate(LocalDate.now().plusDays(1));
    responseDto.setEndDate(LocalDate.now().plusDays(5));
    responseDto.setNextReminderAt(LocalDateTime.now().plusDays(1));
    responseDto.setStatus(TherapyReminderStatus.ACTIVE);
  }

  @Test
  void createReminderShouldReturn201() throws Exception {
    when(therapyReminderService.createReminder(any())).thenReturn(responseDto);

    mockMvc
        .perform(
            post("/api/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "patientProfileId": 10,
                                  "productId": 20,
                                  "dosageInstruction": "Take once daily",
                                  "frequencyPerDay": 1,
                                  "startDate": "2030-01-10",
                                  "endDate": "2030-01-15"
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void createReminderShouldRejectEndDateBeforeStartDate() throws Exception {
    mockMvc
        .perform(
            post("/api/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "patientProfileId": 10,
                                  "productId": 20,
                                  "dosageInstruction": "Take once daily",
                                  "frequencyPerDay": 1,
                                  "startDate": "2030-01-15",
                                  "endDate": "2030-01-10"
                                }
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(therapyReminderService, never()).createReminder(any());
  }
}

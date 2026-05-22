package com.pharmaflow.smartfeatures.controller.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.chat.ChatMessageRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.ChatSessionRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationDeliveryStatusRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationInteractionRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchRequestDto;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.enums.notification.NotificationChannel;
import com.pharmaflow.smartfeatures.enums.notification.NotificationStatus;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.testsupport.SmartFeaturesIntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

class SmartFeaturesIntegrationTest extends SmartFeaturesIntegrationTestSupport {

  @MockBean private UserHealthClient userHealthClient;

  @Test
  void symptomSearchFlowShouldWork() throws Exception {
    PatientHealthProfileSnapshot profile = patientProfile(2L);
    when(userHealthClient.getUser(1L)).thenReturn(Optional.of(user(1L, profile)));
    when(userHealthClient.getPatientProfile(2L)).thenReturn(Optional.of(profile));

    String firstSymptomBody =
        """
                {
                  "name": "Dry Cough",
                  "description": "Persistent dry cough",
                  "severityLevel": "MEDIUM",
                  "isActive": true
                }
                """;

    String symptomResponse =
        mockMvc
            .perform(
                post("/api/symptoms").contentType("application/json").content(firstSymptomBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long symptomId = objectMapper.readTree(symptomResponse).get("id").asLong();

    String secondSymptomResponse =
        mockMvc
            .perform(
                post("/api/symptoms")
                    .contentType("application/json")
                    .content(
                        """
                                {
                                  "name": "Sore Throat",
                                  "description": "Irritated throat",
                                  "severityLevel": "LOW",
                                  "isActive": true
                                }
                                """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long secondSymptomId = objectMapper.readTree(secondSymptomResponse).get("id").asLong();

    mockMvc
        .perform(
            post("/api/symptoms/{symptomId}/matches", symptomId)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new SymptomProductMatchRequestDto(101L, 0.8, "Cough relief"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.productId").value(101L));

    mockMvc
        .perform(
            post("/api/symptoms/{symptomId}/matches", secondSymptomId)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new SymptomProductMatchRequestDto(101L, 0.4, "Throat relief"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.productId").value(101L));

    String searchResponse =
        mockMvc
            .perform(
                post("/api/symptom-searches")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            new SymptomSearchRequestDto(1L, 2L, "Dry cough"))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long searchId = objectMapper.readTree(searchResponse).get("id").asLong();

    mockMvc
        .perform(
            post("/api/symptom-searches/{id}/items", searchId)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(new SymptomSearchItemRequestDto(symptomId))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.symptomId").value(symptomId));

    mockMvc
        .perform(
            post("/api/symptom-searches/{id}/items", searchId)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new SymptomSearchItemRequestDto(secondSymptomId))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.symptomId").value(secondSymptomId));

    mockMvc
        .perform(get("/api/symptom-searches/{id}/matches", searchId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].productId").value(101L))
        .andExpect(jsonPath("$[0].matchedSymptomIds.length()").value(2))
        .andExpect(jsonPath("$[0].relevanceScore").value(greaterThan(0.5)));
  }

  @Test
  void recommendationFlowShouldWork() throws Exception {
    String recommendationBody =
        """
                {
                  "userId": 10,
                  "patientProfileId": 11,
                  "productId": 12,
                  "recommendationType": "FOR_YOU",
                  "score": 0.9,
                  "reasonText": "Personalized pick",
                  "expiresAt": "%s"
                }
                """
            .formatted(LocalDateTime.now().plusDays(3));

    String recommendationResponse =
        mockMvc
            .perform(
                post("/api/recommendations")
                    .contentType("application/json")
                    .content(recommendationBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long recommendationId = objectMapper.readTree(recommendationResponse).get("id").asLong();

    mockMvc
        .perform(
            post("/api/recommendations/{id}/interactions", recommendationId)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new RecommendationInteractionRequestDto(RecommendationEventType.VIEWED))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventType").value("VIEWED"));
  }

  @Test
  void reminderAndNotificationFlowShouldWork() throws Exception {
    String reminderBody =
        """
                {
                  "patientProfileId": 50,
                  "productId": 60,
                  "dosageInstruction": "Take one pill",
                  "frequencyPerDay": 2,
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """
            .formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(7));

    String reminderResponse =
        mockMvc
            .perform(post("/api/reminders").contentType("application/json").content(reminderBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long reminderId = objectMapper.readTree(reminderResponse).get("id").asLong();

    String notificationResponse =
        mockMvc
            .perform(
                post("/api/notifications")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            new NotificationRequestDto(
                                reminderId,
                                70L,
                                50L,
                                NotificationType.THERAPY_REMINDER,
                                NotificationChannel.IN_APP,
                                " Reminder ",
                                " Time for therapy "))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long notificationId = objectMapper.readTree(notificationResponse).get("id").asLong();

    mockMvc
        .perform(
            patch("/api/notifications/{id}/delivery-status", notificationId)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new NotificationDeliveryStatusRequestDto(NotificationStatus.SENT))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SENT"));
  }

  @Test
  void faqAndChatFlowShouldWork() throws Exception {
    mockMvc
        .perform(
            post("/api/faqs")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new FaqEntryRequestDto(
                            "How to pay?",
                            "Use card or cash.",
                            FaqCategory.PAYMENTS,
                            "payment,pay",
                            true))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.isActive").value(true));

    String sessionResponse =
        mockMvc
            .perform(
                post("/api/chat-sessions")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            new ChatSessionRequestDto(300L, 301L, ChatSessionType.FAQ_BOT))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long sessionId = objectMapper.readTree(sessionResponse).get("id").asLong();

    String messageResponse =
        mockMvc
            .perform(
                post("/api/chat-sessions/{id}/messages", sessionId)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            new ChatMessageRequestDto(
                                ChatSenderType.USER, 300L, "How to pay?", null))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long messageId = objectMapper.readTree(messageResponse).get("id").asLong();

    mockMvc
        .perform(get("/api/chat-messages/{messageId}/intent-match", messageId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.detectedIntent").value("PAYMENTS"));
  }

  @Test
  void fraudFlowShouldWork() throws Exception {
    mockMvc
        .perform(
            post("/api/fraud-rules")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new FraudRuleRequestDto(
                            " High quantity ",
                            "ORDER_HIGH_QUANTITY",
                            "ORDER",
                            "Unusually high item quantity",
                            35.0,
                            true))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ruleName").value("High quantity"))
        .andExpect(jsonPath("$.ruleCode").value("ORDER_HIGH_QUANTITY"))
        .andExpect(jsonPath("$.category").value("ORDER"));
  }

  @Test
  void faqValidationShouldReturnFieldError() throws Exception {
    mockMvc
        .perform(
            post("/api/faqs")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new FaqEntryRequestDto(
                            "  ", "Use card", FaqCategory.PAYMENTS, null, true))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.question").exists());
  }

  @Test
  void chatMessageValidationShouldRejectInvalidSenderCombination() throws Exception {
    String sessionResponse =
        mockMvc
            .perform(
                post("/api/chat-sessions")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            new ChatSessionRequestDto(
                                900L, null, ChatSessionType.PHARMACIST_CHAT))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long sessionId = objectMapper.readTree(sessionResponse).get("id").asLong();

    mockMvc
        .perform(
            post("/api/chat-sessions/{id}/messages", sessionId)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ChatMessageRequestDto(ChatSenderType.BOT, 12L, "Automated", null))))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("senderId must not be provided for BOT or SYSTEM senders."));
  }

  private UserHealthSnapshot user(Long id, PatientHealthProfileSnapshot profile) {
    UserHealthSnapshot user = new UserHealthSnapshot();
    user.setId(id);
    user.setPatientProfile(profile);
    return user;
  }

  private PatientHealthProfileSnapshot patientProfile(Long id) {
    PatientHealthProfileSnapshot profile = new PatientHealthProfileSnapshot();
    profile.setId(id);
    return profile;
  }
}

package com.pharmaflow.smartfeatures.config;

import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionStatus;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
import com.pharmaflow.smartfeatures.enums.notification.NotificationChannel;
import com.pharmaflow.smartfeatures.enums.notification.NotificationStatus;
import com.pharmaflow.smartfeatures.enums.notification.NotificationTriggerSource;
import com.pharmaflow.smartfeatures.enums.notification.NotificationType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.model.chat.ChatIntentMatch;
import com.pharmaflow.smartfeatures.model.chat.ChatMessage;
import com.pharmaflow.smartfeatures.model.chat.ChatSession;
import com.pharmaflow.smartfeatures.model.chat.FaqEntry;
import com.pharmaflow.smartfeatures.model.fraud.FraudCheck;
import com.pharmaflow.smartfeatures.model.fraud.FraudLog;
import com.pharmaflow.smartfeatures.model.fraud.FraudRule;
import com.pharmaflow.smartfeatures.model.notification.Notification;
import com.pharmaflow.smartfeatures.model.notification.NotificationTrigger;
import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.model.recommendation.RecommendationEvent;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearchItem;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final int SEED_COUNT = 5;

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        if (alreadySeeded()) {
            return;
        }

        List<Symptom> symptoms = seedSymptoms();
        List<SymptomSearch> searches = seedSymptomSearches();
        seedSymptomSearchItems(searches, symptoms);
        seedSymptomProductMatches(symptoms);

        List<Recommendation> recommendations = seedRecommendations();
        seedRecommendationEvents(recommendations);

        List<TherapyReminder> reminders = seedTherapyReminders();
        List<Notification> notifications = seedNotifications(reminders);
        seedNotificationTriggers(notifications);

        List<FaqEntry> faqEntries = seedFaqEntries();
        List<ChatSession> sessions = seedChatSessions();
        List<ChatMessage> messages = seedChatMessages(sessions);
        seedChatIntentMatches(messages, faqEntries);

        List<FraudRule> fraudRules = seedFraudRules();
        List<FraudCheck> fraudChecks = seedFraudChecks();
        seedFraudLogs(fraudChecks, fraudRules);
    }

    private boolean alreadySeeded() {
        Long count = entityManager.createQuery("select count(s) from Symptom s", Long.class)
                .getSingleResult();
        return count != null && count > 0;
    }

    private List<Symptom> seedSymptoms() {
        List<Symptom> symptoms = new ArrayList<>();
        SymptomSeverityLevel[] severityLevels = SymptomSeverityLevel.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            Symptom symptom = Symptom.builder()
                    .name("Symptom " + i)
                    .description("Seeded symptom " + i)
                    .severityLevel(severityLevels[(i - 1) % severityLevels.length])
                    .isActive(i % 2 != 0)
                    .build();
            persist(symptom);
            symptoms.add(symptom);
        }

        return symptoms;
    }

    private List<SymptomSearch> seedSymptomSearches() {
        List<SymptomSearch> searches = new ArrayList<>();

        for (int i = 1; i <= SEED_COUNT; i++) {
            SymptomSearch search = SymptomSearch.builder()
                    .userId(100L + i)
                    .patientProfileId(200L + i)
                    .searchQuery("search query " + i)
                    .searchedAt(LocalDateTime.now().minusDays(SEED_COUNT - i))
                    .build();
            persist(search);
            searches.add(search);
        }

        return searches;
    }

    private void seedSymptomSearchItems(List<SymptomSearch> searches, List<Symptom> symptoms) {
        for (int i = 0; i < SEED_COUNT; i++) {
            persist(SymptomSearchItem.builder()
                    .search(searches.get(i))
                    .symptom(symptoms.get(i))
                    .build());
        }
    }

    private void seedSymptomProductMatches(List<Symptom> symptoms) {
        for (int i = 0; i < SEED_COUNT; i++) {
            persist(SymptomProductMatch.builder()
                    .symptom(symptoms.get(i))
                    .productId(300L + i)
                    .relevanceScore(0.55 + (i * 0.08))
                    .matchReason("Seeded match reason " + (i + 1))
                    .build());
        }
    }

    private List<Recommendation> seedRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        RecommendationType[] types = RecommendationType.values();
        RecommendationStatus[] statuses = RecommendationStatus.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            Recommendation recommendation = Recommendation.builder()
                    .userId(400L + i)
                    .patientProfileId(500L + i)
                    .productId(600L + i)
                    .recommendationType(types[(i - 1) % types.length])
                    .score(0.60 + (i * 0.05))
                    .reasonText("Seeded recommendation reason " + i)
                    .generatedAt(LocalDateTime.now().minusHours(i))
                    .expiresAt(LocalDateTime.now().plusDays(i))
                    .status(statuses[(i - 1) % statuses.length])
                    .build();
            persist(recommendation);
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    private void seedRecommendationEvents(List<Recommendation> recommendations) {
        RecommendationEventType[] eventTypes = RecommendationEventType.values();

        for (int i = 0; i < SEED_COUNT; i++) {
            persist(RecommendationEvent.builder()
                    .recommendation(recommendations.get(i))
                    .eventType(eventTypes[i % eventTypes.length])
                    .eventTime(LocalDateTime.now().minusMinutes((i + 1) * 10L))
                    .build());
        }
    }

    private List<TherapyReminder> seedTherapyReminders() {
        List<TherapyReminder> reminders = new ArrayList<>();

        for (int i = 1; i <= SEED_COUNT; i++) {
            TherapyReminder reminder = TherapyReminder.builder()
                    .patientProfileId(700L + i)
                    .productId(800L + i)
                    .dosageInstruction("Take " + i + " tablet(s) daily")
                    .frequencyPerDay(i)
                    .startDate(LocalDate.now().minusDays(i))
                    .endDate(LocalDate.now().plusDays(10 + i))
                    .nextReminderAt(LocalDateTime.now().plusHours(i))
                    .status(i % 2 == 0 ? "PAUSED" : "ACTIVE")
                    .build();
            persist(reminder);
            reminders.add(reminder);
        }

        return reminders;
    }

    private List<Notification> seedNotifications(List<TherapyReminder> reminders) {
        List<Notification> notifications = new ArrayList<>();
        NotificationType[] types = NotificationType.values();
        NotificationChannel[] channels = NotificationChannel.values();
        NotificationStatus[] statuses = NotificationStatus.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            Notification notification = Notification.builder()
                    .therapyReminder(reminders.get(i - 1))
                    .userId(900L + i)
                    .patientProfileId(1000L + i)
                    .type(types[(i - 1) % types.length])
                    .title("Notification " + i)
                    .message("Seeded notification message " + i)
                    .channel(channels[(i - 1) % channels.length])
                    .status(statuses[(i - 1) % statuses.length])
                    .createdAt(LocalDateTime.now().minusMinutes(i * 5L))
                    .sentAt(LocalDateTime.now().minusMinutes(i * 4L))
                    .readAt(i % 2 == 0 ? LocalDateTime.now().minusMinutes(i * 3L) : null)
                    .build();
            persist(notification);
            notifications.add(notification);
        }

        return notifications;
    }

    private void seedNotificationTriggers(List<Notification> notifications) {
        NotificationTriggerSource[] sources = NotificationTriggerSource.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            persist(NotificationTrigger.builder()
                    .notification(notifications.get(i - 1))
                    .triggerSource(sources[(i - 1) % sources.length])
                    .sourceEntityId(1100L + i)
                    .triggeredAt(LocalDateTime.now().minusMinutes(i * 2L))
                    .build());
        }
    }

    private List<FaqEntry> seedFaqEntries() {
        List<FaqEntry> faqEntries = new ArrayList<>();
        FaqCategory[] categories = FaqCategory.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            FaqEntry faqEntry = FaqEntry.builder()
                    .question("Question " + i + "?")
                    .answer("Answer " + i)
                    .category(categories[(i - 1) % categories.length])
                    .keywords("keyword" + i + ",common")
                    .isActive(i % 2 != 0)
                    .updatedAt(LocalDateTime.now().minusDays(i))
                    .build();
            persist(faqEntry);
            faqEntries.add(faqEntry);
        }

        return faqEntries;
    }

    private List<ChatSession> seedChatSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        ChatSessionType[] sessionTypes = ChatSessionType.values();
        ChatSessionStatus[] statuses = ChatSessionStatus.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            ChatSession session = ChatSession.builder()
                    .userId(1200L + i)
                    .patientProfileId(1300L + i)
                    .sessionType(sessionTypes[(i - 1) % sessionTypes.length])
                    .status(statuses[(i - 1) % statuses.length])
                    .startedAt(LocalDateTime.now().minusHours(i + 1L))
                    .endedAt(i % 2 == 0 ? LocalDateTime.now().minusHours(i) : null)
                    .build();
            persist(session);
            sessions.add(session);
        }

        return sessions;
    }

    private List<ChatMessage> seedChatMessages(List<ChatSession> sessions) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatSenderType[] senderTypes = ChatSenderType.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            ChatMessage message = ChatMessage.builder()
                    .session(sessions.get(i - 1))
                    .senderType(senderTypes[(i - 1) % senderTypes.length])
                    .senderId(1400L + i)
                    .messageText("Seeded chat message " + i)
                    .attachmentUrl(i % 2 == 0 ? "https://example.com/attachment/" + i : null)
                    .createdAt(LocalDateTime.now().minusMinutes(i * 6L))
                    .build();
            persist(message);
            messages.add(message);
        }

        return messages;
    }

    private void seedChatIntentMatches(List<ChatMessage> messages, List<FaqEntry> faqEntries) {
        for (int i = 1; i <= SEED_COUNT; i++) {
            persist(ChatIntentMatch.builder()
                    .message(messages.get(i - 1))
                    .faqEntry(faqEntries.get(i - 1))
                    .detectedIntent("INTENT_" + i)
                    .confidenceScore(0.70 + (i * 0.04))
                    .build());
        }
    }

    private List<FraudRule> seedFraudRules() {
        List<FraudRule> rules = new ArrayList<>();

        for (int i = 1; i <= SEED_COUNT; i++) {
            FraudRule rule = FraudRule.builder()
                    .ruleName("Rule " + i)
                    .description("Seeded fraud rule " + i)
                    .weight(0.25 + (i * 0.15))
                    .isActive(i % 2 != 0)
                    .build();
            persist(rule);
            rules.add(rule);
        }

        return rules;
    }

    private List<FraudCheck> seedFraudChecks() {
        List<FraudCheck> checks = new ArrayList<>();
        FraudDecision[] decisions = FraudDecision.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            FraudCheck check = FraudCheck.builder()
                    .userId(1500L + i)
                    .orderId(1600L + i)
                    .riskScore(0.35 + (i * 0.10))
                    .decision(decisions[(i - 1) % decisions.length])
                    .checkedAt(LocalDateTime.now().minusMinutes(i * 7L))
                    .build();
            persist(check);
            checks.add(check);
        }

        return checks;
    }

    private void seedFraudLogs(List<FraudCheck> checks, List<FraudRule> rules) {
        FraudEventType[] eventTypes = FraudEventType.values();

        for (int i = 1; i <= SEED_COUNT; i++) {
            persist(FraudLog.builder()
                    .fraudCheck(checks.get(i - 1))
                    .fraudRule(rules.get(i - 1))
                    .eventType(eventTypes[(i - 1) % eventTypes.length])
                    .details("Seeded fraud log " + i)
                    .createdAt(LocalDateTime.now().minusMinutes(i * 8L))
                    .build());
        }
    }

    private void persist(Object entity) {
        entityManager.persist(entity);
    }
}

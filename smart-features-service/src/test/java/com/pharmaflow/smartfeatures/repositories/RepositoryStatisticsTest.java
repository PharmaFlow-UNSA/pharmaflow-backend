package com.pharmaflow.smartfeatures.repositories;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
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
import com.pharmaflow.smartfeatures.repositories.chat.ChatIntentMatchRepository;
import com.pharmaflow.smartfeatures.repositories.chat.ChatMessageRepository;
import com.pharmaflow.smartfeatures.repositories.chat.ChatSessionRepository;
import com.pharmaflow.smartfeatures.repositories.chat.FaqEntryRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudCheckRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudLogRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudRuleRepository;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationRepository;
import com.pharmaflow.smartfeatures.repositories.notification.NotificationTriggerRepository;
import com.pharmaflow.smartfeatures.repositories.notification.TherapyReminderRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationEventRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomProductMatchRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchItemRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class RepositoryStatisticsTest {

  @Autowired private EntityManager entityManager;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private ChatSessionRepository chatSessionRepository;

  @Autowired private ChatMessageRepository chatMessageRepository;

  @Autowired private ChatIntentMatchRepository chatIntentMatchRepository;

  @Autowired private FaqEntryRepository faqEntryRepository;

  @Autowired private FraudCheckRepository fraudCheckRepository;

  @Autowired private FraudLogRepository fraudLogRepository;

  @Autowired private FraudRuleRepository fraudRuleRepository;

  @Autowired private NotificationRepository notificationRepository;

  @Autowired private NotificationTriggerRepository notificationTriggerRepository;

  @Autowired private TherapyReminderRepository therapyReminderRepository;

  @Autowired private RecommendationRepository recommendationRepository;

  @Autowired private RecommendationEventRepository recommendationEventRepository;

  @Autowired private SymptomRepository symptomRepository;

  @Autowired private SymptomProductMatchRepository symptomProductMatchRepository;

  @Autowired private SymptomSearchRepository symptomSearchRepository;

  @Autowired private SymptomSearchItemRepository symptomSearchItemRepository;

  private Statistics statistics;
  private Long notificationId;
  private Long reminderId;
  private Long recommendationId;
  private Long searchId;
  private Long symptomId;
  private Long fraudCheckId;
  private Long sessionId;
  private Long messageId;

  @BeforeEach
  void setUp() {
    statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    deleteAllData();
    seedData();
    entityManager.flush();
    entityManager.clear();
    statistics.clear();
  }

  @Test
  void notificationRepositoryShouldAvoidNPlusOneWhenAccessingReminder() {
    List<Notification> notifications =
        notificationRepository.findByUserIdOrderByCreatedAtDesc(100L);

    assertThat(notifications).hasSize(2);
    assertThat(notifications)
        .extracting(notification -> notification.getTherapyReminder().getReminderId())
        .containsOnly(reminderId);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
  }

  @Test
  void notificationTriggerRepositoryShouldAvoidNPlusOneWhenAccessingNotification() {
    List<NotificationTrigger> triggers =
        notificationTriggerRepository.findByNotificationNotificationIdOrderByTriggeredAtDesc(
            notificationId);

    assertThat(triggers).hasSize(2);
    assertThat(triggers)
        .extracting(trigger -> trigger.getNotification().getNotificationId())
        .containsOnly(notificationId);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
  }

  @Test
  void recommendationEventRepositoryShouldAvoidNPlusOneWhenAccessingRecommendation() {
    List<RecommendationEvent> events =
        recommendationEventRepository.findByRecommendationRecommendationIdOrderByEventTimeDesc(
            recommendationId);

    assertThat(events).hasSize(2);
    assertThat(events)
        .extracting(event -> event.getRecommendation().getRecommendationId())
        .containsOnly(recommendationId);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
  }

  @Test
  void symptomSearchItemRepositoryShouldAvoidNPlusOneWhenAccessingSearchAndSymptom() {
    List<SymptomSearchItem> items =
        symptomSearchItemRepository.findBySearchSearchIdOrderBySearchItemIdAsc(searchId);

    assertThat(items).hasSize(2);
    assertThat(items).extracting(item -> item.getSearch().getSearchId()).containsOnly(searchId);
    assertThat(items)
        .extracting(item -> item.getSymptom().getName())
        .containsExactly("Dry Cough", "Fever");
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
  }

  @Test
  void symptomProductMatchRepositoryShouldAvoidNPlusOneWhenAccessingSymptom() {
    List<SymptomProductMatch> matches =
        symptomProductMatchRepository.findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(
            symptomId);

    assertThat(matches).hasSize(2);
    assertThat(matches)
        .extracting(match -> match.getSymptom().getSymptomId())
        .containsOnly(symptomId);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
  }

  @Test
  void fraudLogRepositoryShouldAvoidNPlusOneWhenAccessingCheckAndRule() {
    List<FraudLog> logs =
        fraudLogRepository.findByFraudCheckFraudCheckIdOrderByCreatedAtDesc(fraudCheckId);

    assertThat(logs).hasSize(2);
    assertThat(logs)
        .extracting(log -> log.getFraudCheck().getFraudCheckId())
        .containsOnly(fraudCheckId);
    assertThat(logs)
        .extracting(log -> log.getFraudRule().getRuleName())
        .containsExactly("Late pickup", "High value");
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
  }

  @Test
  void chatMessageRepositoryShouldAvoidNPlusOneWhenAccessingSession() {
    List<ChatMessage> messages =
        chatMessageRepository.findBySessionSessionIdOrderByCreatedAtAsc(sessionId);
    long queriesAfterFetch = statistics.getPrepareStatementCount();

    assertThat(messages).hasSize(2);
    assertThat(messages)
        .extracting(message -> message.getSession().getSessionId())
        .containsOnly(sessionId);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(queriesAfterFetch);
    assertThat(queriesAfterFetch).isLessThanOrEqualTo(3L);
  }

  @Test
  void chatIntentMatchRepositoryShouldFetchMessageAndFaqInSingleQuery() {
    ChatIntentMatch intentMatch =
        chatIntentMatchRepository.findByMessageMessageId(messageId).orElseThrow();

    assertThat(intentMatch.getMessage().getMessageId()).isEqualTo(messageId);
    assertThat(intentMatch.getFaqEntry().getQuestion()).isEqualTo("How to pay?");
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
  }

  private void seedData() {
    TherapyReminder reminder =
        therapyReminderRepository.save(
            TherapyReminder.builder()
                .ownerUserId(100L)
                .patientProfileId(200L)
                .productId(300L)
                .dosageInstruction("Take once daily")
                .frequencyPerDay(1)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(7))
                .nextReminderAt(LocalDateTime.now().plusDays(1))
                .status(TherapyReminderStatus.ACTIVE)
                .build());
    reminderId = reminder.getReminderId();

    Notification firstNotification =
        notificationRepository.save(
            Notification.builder()
                .therapyReminder(reminder)
                .userId(100L)
                .patientProfileId(200L)
                .type(NotificationType.THERAPY_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .title("Reminder")
                .message("Time for therapy")
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build());
    Notification secondNotification =
        notificationRepository.save(
            Notification.builder()
                .therapyReminder(reminder)
                .userId(100L)
                .patientProfileId(200L)
                .type(NotificationType.THERAPY_REMINDER)
                .channel(NotificationChannel.EMAIL)
                .title("Follow up")
                .message("Second reminder")
                .status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .sentAt(LocalDateTime.now().minusSeconds(30))
                .build());
    notificationId = secondNotification.getNotificationId();

    notificationTriggerRepository.saveAll(
        List.of(
            NotificationTrigger.builder()
                .notification(secondNotification)
                .triggerSource(NotificationTriggerSource.THERAPY)
                .sourceEntityId(reminderId)
                .triggeredAt(LocalDateTime.now().minusSeconds(20))
                .build(),
            NotificationTrigger.builder()
                .notification(secondNotification)
                .triggerSource(NotificationTriggerSource.THERAPY)
                .sourceEntityId(reminderId)
                .triggeredAt(LocalDateTime.now().minusSeconds(10))
                .build()));

    Recommendation recommendation =
        recommendationRepository.save(
            Recommendation.builder()
                .userId(400L)
                .patientProfileId(401L)
                .productId(402L)
                .recommendationType(RecommendationType.FOR_YOU)
                .score(0.95)
                .reasonText("Relevant for you")
                .generatedAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusDays(2))
                .status(RecommendationStatus.ACTIVE)
                .build());
    recommendationId = recommendation.getRecommendationId();

    recommendationEventRepository.saveAll(
        List.of(
            RecommendationEvent.builder()
                .recommendation(recommendation)
                .eventType(RecommendationEventType.VIEWED)
                .eventTime(LocalDateTime.now().minusMinutes(2))
                .build(),
            RecommendationEvent.builder()
                .recommendation(recommendation)
                .eventType(RecommendationEventType.CLICKED)
                .eventTime(LocalDateTime.now().minusMinutes(1))
                .build()));

    Symptom dryCough =
        symptomRepository.save(
            Symptom.builder()
                .name("Dry Cough")
                .description("Persistent cough")
                .severityLevel(SymptomSeverityLevel.MEDIUM)
                .isActive(true)
                .build());
    Symptom fever =
        symptomRepository.save(
            Symptom.builder()
                .name("Fever")
                .description("Elevated temperature")
                .severityLevel(SymptomSeverityLevel.HIGH)
                .isActive(true)
                .build());
    symptomId = dryCough.getSymptomId();

    symptomProductMatchRepository.saveAll(
        List.of(
            SymptomProductMatch.builder()
                .symptom(dryCough)
                .productId(501L)
                .relevanceScore(0.9)
                .matchReason("Targets dry cough")
                .build(),
            SymptomProductMatch.builder()
                .symptom(dryCough)
                .productId(502L)
                .relevanceScore(0.7)
                .matchReason("Night relief")
                .build()));

    SymptomSearch search =
        symptomSearchRepository.save(
            SymptomSearch.builder()
                .userId(600L)
                .patientProfileId(601L)
                .searchQuery("dry cough and fever")
                .searchedAt(LocalDateTime.now().minusMinutes(5))
                .build());
    searchId = search.getSearchId();

    symptomSearchItemRepository.saveAll(
        List.of(
            SymptomSearchItem.builder().search(search).symptom(dryCough).build(),
            SymptomSearchItem.builder().search(search).symptom(fever).build()));

    FraudRule highValueRule =
        fraudRuleRepository.save(
            FraudRule.builder()
                .ruleName("High value")
                .ruleCode("ORDER_HIGH_QUANTITY")
                .category("ORDER")
                .description("High order total")
                .weight(45.0)
                .isActive(true)
                .build());
    FraudRule latePickupRule =
        fraudRuleRepository.save(
            FraudRule.builder()
                .ruleName("Late pickup")
                .ruleCode("ORDER_VELOCITY")
                .category("ORDER")
                .description("Repeated delayed pickup")
                .weight(20.0)
                .isActive(true)
                .build());

    FraudCheck fraudCheck =
        fraudCheckRepository.save(
            FraudCheck.builder()
                .userId(700L)
                .orderId(701L)
                .riskScore(65.0)
                .decision(FraudDecision.REVIEW)
                .checkedAt(LocalDateTime.now().minusMinutes(10))
                .build());
    fraudCheckId = fraudCheck.getFraudCheckId();

    fraudLogRepository.saveAll(
        List.of(
            FraudLog.builder()
                .fraudCheck(fraudCheck)
                .fraudRule(highValueRule)
                .eventType(FraudEventType.TRIGGERED)
                .details("Triggered by amount")
                .scoreContribution(45.0)
                .createdAt(LocalDateTime.now().minusMinutes(4))
                .build(),
            FraudLog.builder()
                .fraudCheck(fraudCheck)
                .fraudRule(latePickupRule)
                .eventType(FraudEventType.CLEARED)
                .details("Not triggered")
                .scoreContribution(0.0)
                .createdAt(LocalDateTime.now().minusMinutes(3))
                .build()));

    FaqEntry faqEntry =
        faqEntryRepository.save(
            FaqEntry.builder()
                .question("How to pay?")
                .answer("Use card or cash.")
                .category(FaqCategory.PAYMENTS)
                .keywords("payment,pay")
                .isActive(true)
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build());

    ChatSession session =
        chatSessionRepository.save(
            ChatSession.builder()
                .userId(800L)
                .patientProfileId(801L)
                .sessionType(ChatSessionType.FAQ_BOT)
                .status(ChatSessionStatus.OPEN)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .build());
    sessionId = session.getSessionId();

    ChatMessage firstMessage =
        chatMessageRepository.save(
            ChatMessage.builder()
                .session(session)
                .senderType(ChatSenderType.USER)
                .senderId(800L)
                .messageText("How to pay?")
                .createdAt(LocalDateTime.now().minusMinutes(29))
                .build());
    ChatMessage secondMessage =
        chatMessageRepository.save(
            ChatMessage.builder()
                .session(session)
                .senderType(ChatSenderType.BOT)
                .messageText("Use card or cash.")
                .createdAt(LocalDateTime.now().minusMinutes(28))
                .build());
    messageId = firstMessage.getMessageId();

    chatIntentMatchRepository.save(
        ChatIntentMatch.builder()
            .message(firstMessage)
            .faqEntry(faqEntry)
            .detectedIntent("PAYMENTS")
            .confidenceScore(0.95)
            .build());

    assertThat(secondMessage.getMessageId()).isNotNull();
  }

  private void deleteAllData() {
    chatIntentMatchRepository.deleteAll();
    chatMessageRepository.deleteAll();
    chatSessionRepository.deleteAll();
    faqEntryRepository.deleteAll();

    fraudLogRepository.deleteAll();
    fraudCheckRepository.deleteAll();
    fraudRuleRepository.deleteAll();

    notificationTriggerRepository.deleteAll();
    notificationRepository.deleteAll();
    therapyReminderRepository.deleteAll();

    recommendationEventRepository.deleteAll();
    recommendationRepository.deleteAll();

    symptomSearchItemRepository.deleteAll();
    symptomProductMatchRepository.deleteAll();
    symptomSearchRepository.deleteAll();
    symptomRepository.deleteAll();
  }
}

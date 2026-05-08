package com.pharmaflow.smartfeatures.config;

import com.pharmaflow.smartfeatures.enums.chat.ChatSenderType;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionStatus;
import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
import com.pharmaflow.smartfeatures.enums.fraud.FraudRuleCode;
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
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
      seedMissingFaqEntries();
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
    Long count =
        entityManager.createQuery("select count(s) from Symptom s", Long.class).getSingleResult();
    return count != null && count > 0;
  }

  private List<Symptom> seedSymptoms() {
    List<Symptom> symptoms = new ArrayList<>();
    SymptomSeverityLevel[] severityLevels = SymptomSeverityLevel.values();

    for (int i = 1; i <= SEED_COUNT; i++) {
      Symptom symptom =
          Symptom.builder()
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
      SymptomSearch search =
          SymptomSearch.builder()
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
      persist(SymptomSearchItem.builder().search(searches.get(i)).symptom(symptoms.get(i)).build());
    }
  }

  private void seedSymptomProductMatches(List<Symptom> symptoms) {
    for (int i = 0; i < SEED_COUNT; i++) {
      persist(
          SymptomProductMatch.builder()
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
      Recommendation recommendation =
          Recommendation.builder()
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
      persist(
          RecommendationEvent.builder()
              .recommendation(recommendations.get(i))
              .eventType(eventTypes[i % eventTypes.length])
              .eventTime(LocalDateTime.now().minusMinutes((i + 1) * 10L))
              .build());
    }
  }

  private List<TherapyReminder> seedTherapyReminders() {
    List<TherapyReminder> reminders = new ArrayList<>();

    for (int i = 1; i <= SEED_COUNT; i++) {
      TherapyReminder reminder =
          TherapyReminder.builder()
              .patientProfileId(700L + i)
              .productId(800L + i)
              .dosageInstruction("Take " + i + " tablet(s) daily")
              .frequencyPerDay(i)
              .startDate(LocalDate.now().minusDays(i))
              .endDate(LocalDate.now().plusDays(10 + i))
              .nextReminderAt(LocalDateTime.now().plusHours(i))
              .status(i % 2 == 0 ? TherapyReminderStatus.PAUSED : TherapyReminderStatus.ACTIVE)
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
      Notification notification =
          Notification.builder()
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
      persist(
          NotificationTrigger.builder()
              .notification(notifications.get(i - 1))
              .triggerSource(sources[(i - 1) % sources.length])
              .sourceEntityId(1100L + i)
              .triggeredAt(LocalDateTime.now().minusMinutes(i * 2L))
              .build());
    }
  }

  private List<FaqEntry> seedFaqEntries() {
    List<FaqEntry> faqEntries = new ArrayList<>();
    for (FaqSeed faqSeed : faqSeeds()) {
      FaqEntry faqEntry = toFaqEntry(faqSeed);
      persist(faqEntry);
      faqEntries.add(faqEntry);
    }
    return faqEntries;
  }

  private void seedMissingFaqEntries() {
    for (FaqSeed faqSeed : faqSeeds()) {
      if (!faqExists(faqSeed.question())) {
        persist(toFaqEntry(faqSeed));
      }
    }
  }

  private boolean faqExists(String question) {
    Long count =
        entityManager
            .createQuery(
                "select count(f) from FaqEntry f where f.normalizedQuestion = :normalizedQuestion",
                Long.class)
            .setParameter("normalizedQuestion", TextSanitizer.normalizeKey(question))
            .getSingleResult();
    return count != null && count > 0;
  }

  private FaqEntry toFaqEntry(FaqSeed faqSeed) {
    return FaqEntry.builder()
        .question(faqSeed.question())
        .answer(faqSeed.answer())
        .category(faqSeed.category())
        .keywords(faqSeed.keywords())
        .isActive(true)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private List<FaqSeed> faqSeeds() {
    return List.of(
        new FaqSeed(
            "How can I track my order?",
            "You can track your order from your PharmaFlow account order history once the pharmacy confirms it.",
            FaqCategory.ORDERS,
            "order,status,tracking,history,confirmed"),
        new FaqSeed(
            "Can I cancel an order before it is prepared?",
            "You can cancel an order while it is still pending. Once the pharmacy starts preparing it, contact support for help.",
            FaqCategory.ORDERS,
            "order,cancel,pending,prepared,support"),
        new FaqSeed(
            "Why is my order still pending?",
            "Orders can remain pending while the pharmacy checks stock, validates prescription requirements, or confirms payment.",
            FaqCategory.ORDERS,
            "order,pending,stock,prescription,payment"),
        new FaqSeed(
            "What happens if an item in my order is unavailable?",
            "If an item is unavailable, the pharmacy may reject that item or contact you with next steps before fulfillment.",
            FaqCategory.ORDERS,
            "order,unavailable,out of stock,item,fulfillment"),
        new FaqSeed(
            "Can I reorder a previous purchase?",
            "You can use your order history as a reference when placing a new order, subject to stock and prescription requirements.",
            FaqCategory.ORDERS,
            "order,reorder,previous purchase,history,stock"),
        new FaqSeed(
            "How do I know if my order was confirmed?",
            "A confirmed order appears in your order history with its current status and pharmacy fulfillment details.",
            FaqCategory.ORDERS,
            "order,confirmed,status,history,pharmacy"),
        new FaqSeed(
            "Can I change items after placing an order?",
            "Items usually cannot be changed after placement. Cancel the pending order if possible and create a new one.",
            FaqCategory.ORDERS,
            "order,change items,edit,cancel,new order"),
        new FaqSeed(
            "Why was my order rejected?",
            "An order can be rejected because of unavailable stock, prescription issues, payment failure, or pharmacy review results.",
            FaqCategory.ORDERS,
            "order,rejected,stock,prescription,payment,review"),
        new FaqSeed(
            "Can I place an order for a family member?",
            "You can order for a family member when their profile and any required prescription information are available in PharmaFlow.",
            FaqCategory.ORDERS,
            "order,family member,profile,prescription"),
        new FaqSeed(
            "Where can I see my order history?",
            "Your order history is available from your PharmaFlow account and shows previous orders and their statuses.",
            FaqCategory.ORDERS,
            "order history,previous orders,status,account"),
        new FaqSeed(
            "Can I upload my prescription as a PDF?",
            "Yes. You can upload a prescription as a PDF or image, and the pharmacy team will review it before fulfillment.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,pdf,image,upload,review"),
        new FaqSeed(
            "Which prescription file formats are supported?",
            "PharmaFlow supports common prescription image and PDF uploads when the file is readable and complete.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,file format,pdf,image,upload"),
        new FaqSeed(
            "How long does prescription review take?",
            "Prescription review time depends on pharmacy workload and prescription clarity. The order status updates after review.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,review,time,pharmacy,status"),
        new FaqSeed(
            "Why was my prescription rejected?",
            "A prescription may be rejected if it is unreadable, incomplete, expired, or does not meet pharmacy requirements.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,rejected,unreadable,incomplete,expired"),
        new FaqSeed(
            "Can I reuse an old prescription?",
            "Prescription reuse depends on pharmacy rules and prescription validity. Upload the prescription so the pharmacy can review it.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,reuse,old,validity,review"),
        new FaqSeed(
            "Can I edit a prescription after uploading it?",
            "If you uploaded the wrong prescription, cancel the pending order when possible and submit the correct document.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,edit,wrong upload,cancel,document"),
        new FaqSeed(
            "Can the chatbot tell me how much medicine to take?",
            "No. PharmaFlow cannot provide dosage instructions. For medical advice, contact a licensed pharmacist or doctor.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,dosage,medical advice,pharmacist,doctor"),
        new FaqSeed(
            "Do I need a prescription for every medicine?",
            "Some products require a valid prescription while others do not. Product and pharmacy rules determine what is required.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,required,medicine,product,pharmacy"),
        new FaqSeed(
            "Can I upload multiple prescriptions?",
            "You can upload the prescription documents needed for your order as long as each document is clear and relevant.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,multiple documents,upload,clear,relevant"),
        new FaqSeed(
            "Who reviews my prescription?",
            "The pharmacy team reviews prescription documents before dispensing prescription-required products.",
            FaqCategory.PRESCRIPTIONS,
            "prescription,review,pharmacy team,dispensing"),
        new FaqSeed(
            "How long does delivery usually take?",
            "Delivery time depends on pharmacy availability and address, but confirmed orders show the latest delivery estimate in the app.",
            FaqCategory.DELIVERY,
            "delivery,time,address,estimate,confirmed order"),
        new FaqSeed(
            "Can I change my delivery address?",
            "You can change the address before fulfillment when the order is still pending. After that, contact support.",
            FaqCategory.DELIVERY,
            "delivery,address,change,pending,fulfillment"),
        new FaqSeed(
            "What should I do if my delivery is late?",
            "Check the latest order status first. If the delivery is still late, contact support with your order number.",
            FaqCategory.DELIVERY,
            "delivery,late,delay,status,order number"),
        new FaqSeed(
            "Can I choose a delivery time window?",
            "Available delivery time windows depend on pharmacy and courier options shown during checkout.",
            FaqCategory.DELIVERY,
            "delivery,time window,courier,checkout,pharmacy"),
        new FaqSeed(
            "Do all pharmacies offer delivery?",
            "Delivery availability depends on the selected pharmacy, delivery address, and current service coverage.",
            FaqCategory.DELIVERY,
            "delivery,pharmacy,coverage,address,availability"),
        new FaqSeed(
            "Can I pick up my order instead of delivery?",
            "Pickup availability depends on the pharmacy options shown during checkout.",
            FaqCategory.DELIVERY,
            "pickup,delivery,pharmacy,checkout"),
        new FaqSeed(
            "What happens if I am not home for delivery?",
            "Courier handling can vary. Check the delivery status or contact support for the next available step.",
            FaqCategory.DELIVERY,
            "delivery,not home,courier,status,support"),
        new FaqSeed(
            "Can refrigerated medicine be delivered?",
            "Temperature-sensitive delivery depends on pharmacy and courier capability. The pharmacy confirms availability during review.",
            FaqCategory.DELIVERY,
            "delivery,refrigerated,temperature sensitive,courier,review"),
        new FaqSeed(
            "How do I see delivery fees?",
            "Delivery fees, when applicable, are shown during checkout before you confirm the order.",
            FaqCategory.DELIVERY,
            "delivery fee,checkout,cost,confirm order"),
        new FaqSeed(
            "Can I track the courier in real time?",
            "Real-time courier tracking depends on the delivery provider. Use the order status for available tracking updates.",
            FaqCategory.DELIVERY,
            "delivery,courier,real time tracking,status"),
        new FaqSeed(
            "Which payment methods are accepted?",
            "PharmaFlow supports the payment methods shown at checkout. Availability can vary by pharmacy and delivery option.",
            FaqCategory.PAYMENTS,
            "payment,card,cash,checkout,receipt"),
        new FaqSeed(
            "When will I be charged for my order?",
            "Payment timing depends on the selected payment method and pharmacy confirmation flow shown at checkout.",
            FaqCategory.PAYMENTS,
            "payment,charged,checkout,confirmation"),
        new FaqSeed(
            "Why did my payment fail?",
            "Payment can fail because of card issues, bank rejection, insufficient funds, or temporary payment provider errors.",
            FaqCategory.PAYMENTS,
            "payment failed,card,bank,insufficient funds,error"),
        new FaqSeed(
            "Can I pay when the order is delivered?",
            "Pay-on-delivery is available only when that option appears during checkout for the selected pharmacy.",
            FaqCategory.PAYMENTS,
            "cash on delivery,pay on delivery,checkout,pharmacy"),
        new FaqSeed(
            "Where can I find my receipt?",
            "Receipts are available from your order details when the payment and order flow support receipt generation.",
            FaqCategory.PAYMENTS,
            "receipt,invoice,order details,payment"),
        new FaqSeed(
            "Can I use insurance in PharmaFlow?",
            "Insurance support depends on pharmacy and platform availability. Check checkout details or contact support.",
            FaqCategory.PAYMENTS,
            "insurance,payment,checkout,support"),
        new FaqSeed(
            "Can I split payment across two cards?",
            "Split payments are only supported if the option appears during checkout.",
            FaqCategory.PAYMENTS,
            "split payment,two cards,checkout"),
        new FaqSeed(
            "How do refunds work?",
            "Refund handling depends on order status, pharmacy review, and payment method. Contact support for refund-specific help.",
            FaqCategory.PAYMENTS,
            "refund,payment,order status,support"),
        new FaqSeed(
            "Why is there a price difference at checkout?",
            "Prices can change because of pharmacy selection, stock updates, delivery fees, or final checkout rules.",
            FaqCategory.PAYMENTS,
            "price,checkout,delivery fee,stock,pharmacy"),
        new FaqSeed(
            "Can I download an invoice?",
            "If invoices are available for your order, you can access them from the order details page.",
            FaqCategory.PAYMENTS,
            "invoice,download,order details,receipt"),
        new FaqSeed(
            "How do I reset my password?",
            "Use the password reset option on the sign-in screen and follow the instructions sent to your registered contact method.",
            FaqCategory.ACCOUNT,
            "account,password,reset,login,sign in"),
        new FaqSeed(
            "How do I update my profile information?",
            "You can update supported profile details from your PharmaFlow account settings.",
            FaqCategory.ACCOUNT,
            "account,profile,update,settings"),
        new FaqSeed(
            "Can I add family members to my account?",
            "Family member support depends on your account settings. Add or manage family profiles from the health profile area.",
            FaqCategory.ACCOUNT,
            "account,family member,profile,health profile"),
        new FaqSeed(
            "How do I update allergy information?",
            "Update allergy information from the health profile area so pharmacies can review the latest recorded details.",
            FaqCategory.ACCOUNT,
            "account,allergy,health profile,update,pharmacy"),
        new FaqSeed(
            "How do I contact support?",
            "Use the support option in PharmaFlow and include relevant order, account, or pharmacy details.",
            FaqCategory.ACCOUNT,
            "account,support,contact,help"),
        new FaqSeed(
            "Can I delete my account?",
            "Account deletion options depend on platform policy and data retention requirements. Contact support for help.",
            FaqCategory.ACCOUNT,
            "account,delete,data retention,support"),
        new FaqSeed(
            "Why can I not log in?",
            "Login issues can happen because of incorrect credentials, password reset requirements, or temporary service problems.",
            FaqCategory.ACCOUNT,
            "account,login,credentials,password,service problem"),
        new FaqSeed(
            "How do I change my email address?",
            "If email changes are supported, update your email from account settings or contact support for assistance.",
            FaqCategory.ACCOUNT,
            "account,email,change,settings,support"),
        new FaqSeed(
            "Can I manage notifications?",
            "You can manage supported notification preferences from your account or app settings.",
            FaqCategory.ACCOUNT,
            "account,notifications,preferences,settings"),
        new FaqSeed(
            "Is my health information shared with pharmacies?",
            "Relevant health information may be used for pharmacy review when needed to support your order and safety checks.",
            FaqCategory.ACCOUNT,
            "account,health information,privacy,pharmacy,safety"));
  }

  private record FaqSeed(String question, String answer, FaqCategory category, String keywords) {}

  private List<ChatSession> seedChatSessions() {
    List<ChatSession> sessions = new ArrayList<>();
    ChatSessionType[] sessionTypes = ChatSessionType.values();
    ChatSessionStatus[] statuses = ChatSessionStatus.values();

    for (int i = 1; i <= SEED_COUNT; i++) {
      ChatSession session =
          ChatSession.builder()
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
      ChatMessage message =
          ChatMessage.builder()
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
      persist(
          ChatIntentMatch.builder()
              .message(messages.get(i - 1))
              .faqEntry(faqEntries.get(i - 1))
              .detectedIntent("INTENT_" + i)
              .confidenceScore(0.70 + (i * 0.04))
              .build());
    }
  }

  private List<FraudRule> seedFraudRules() {
    List<FraudRule> rules = new ArrayList<>();
    FraudRuleCode[] ruleCodes = FraudRuleCode.values();

    for (int i = 1; i <= SEED_COUNT; i++) {
      FraudRuleCode ruleCode = ruleCodes[i - 1];
      FraudRule rule =
          FraudRule.builder()
              .ruleName(ruleCode.getDefaultRuleName())
              .ruleCode(ruleCode.name())
              .category(ruleCode.getCategory())
              .description("Seeded fraud rule " + i)
              .weight(ruleCode.getDefaultWeight())
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
      FraudCheck check =
          FraudCheck.builder()
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
      persist(
          FraudLog.builder()
              .fraudCheck(checks.get(i - 1))
              .fraudRule(rules.get(i - 1))
              .eventType(eventTypes[(i - 1) % eventTypes.length])
              .details("Seeded fraud log " + i)
              .scoreContribution(i % 2 == 0 ? 0.0 : rules.get(i - 1).getWeight())
              .createdAt(LocalDateTime.now().minusMinutes(i * 8L))
              .build());
    }
  }

  private void persist(Object entity) {
    entityManager.persist(entity);
  }
}

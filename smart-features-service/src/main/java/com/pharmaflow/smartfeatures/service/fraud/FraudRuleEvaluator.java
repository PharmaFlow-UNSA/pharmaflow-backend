package com.pharmaflow.smartfeatures.service.fraud;

import com.pharmaflow.smartfeatures.client.fraud.FraudOrderClient;
import com.pharmaflow.smartfeatures.client.fraud.FraudProductClient;
import com.pharmaflow.smartfeatures.client.fraud.FraudUserClient;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalCategoryDto;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalOrderDto;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalOrderItemDto;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalPrescriptionDto;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalProductDto;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalSubstanceDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
import com.pharmaflow.smartfeatures.enums.fraud.FraudRuleCode;
import com.pharmaflow.smartfeatures.model.fraud.FraudRule;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FraudRuleEvaluator {

  private static final int HIGH_LINE_QUANTITY = 10;
  private static final int HIGH_TOTAL_QUANTITY = 20;
  private static final int ORDER_VELOCITY_THRESHOLD = 3;
  private static final int FAILED_PAYMENT_THRESHOLD = 3;
  private static final int RESTRICTED_PRODUCT_ORDER_THRESHOLD = 3;
  private static final int SHARED_ADDRESS_USER_THRESHOLD = 3;
  private static final int REJECTED_PRESCRIPTION_THRESHOLD = 3;
  private static final int PRESCRIPTION_REUSE_THRESHOLD = 2;
  private static final int CONTROLLED_QUANTITY_THRESHOLD = 5;

  private final FraudOrderClient orderClient;
  private final FraudProductClient productClient;
  private final FraudUserClient userClient;

  public FraudRuleEvaluator(
      FraudOrderClient orderClient, FraudProductClient productClient, FraudUserClient userClient) {
    this.orderClient = orderClient;
    this.productClient = productClient;
    this.userClient = userClient;
  }

  public FraudEvaluation evaluate(Long orderId, List<FraudRule> rules) {
    FraudContext context = loadContext(orderId);
    List<RuleOutcome> outcomes = rules.stream().map(rule -> evaluateRule(rule, context)).toList();
    double riskScore = outcomes.stream().mapToDouble(RuleOutcome::scoreContribution).sum();
    return new FraudEvaluation(context.order().getUserId(), Math.min(riskScore, 100.0), outcomes);
  }

  private FraudContext loadContext(Long orderId) {
    ExternalOrderDto order = orderClient.getOrder(orderId);
    Long userId = order.getUserId();
    if (userId == null) {
      throw new IllegalStateException(
          "Order service returned order without userId for order id: " + orderId);
    }

    List<ExternalOrderDto> userOrders = orderClient.getOrdersByUser(userId);
    List<ExternalOrderDto> allOrders = orderClient.getAllOrders();
    List<ExternalPrescriptionDto> userPrescriptions = orderClient.getPrescriptionsByUser(userId);
    List<ExternalPrescriptionDto> allPrescriptions = orderClient.getAllPrescriptions();
    userClient.getUser(userId);

    Map<Long, ExternalProductDto> productsById = new HashMap<>();
    for (ExternalOrderItemDto item : safeItems(order)) {
      Long productId = item.getProductId();
      if (productId != null && !productsById.containsKey(productId)) {
        productClient
            .getProduct(productId)
            .ifPresent(product -> productsById.put(productId, product));
      }
    }

    return new FraudContext(
        order, userOrders, allOrders, userPrescriptions, allPrescriptions, productsById);
  }

  private RuleOutcome evaluateRule(FraudRule rule, FraudContext context) {
    Optional<FraudRuleCode> ruleCode = FraudRuleCode.fromCode(rule.getRuleCode());
    if (ruleCode.isEmpty()) {
      return skipped(rule, "Rule code is not supported by the local evaluator.");
    }

    return switch (ruleCode.get()) {
      case ORDER_HIGH_QUANTITY -> evaluateHighQuantity(rule, context);
      case ORDER_VELOCITY -> evaluateOrderVelocity(rule, context);
      case PAYMENT_REPEATED_FAILURES -> evaluateRepeatedFailedPayments(rule, context);
      case USER_RESTRICTED_PRODUCT_FREQUENCY -> evaluateRestrictedProductFrequency(rule, context);
      case ACCOUNT_SHARED_CONTACT -> evaluateSharedContact(rule, context);
      case ACCOUNT_NEW_LARGE_ORDER ->
          skipped(rule, "Missing upstream signal: user API does not expose account createdAt.");
      case ACCOUNT_SUSPICIOUS_ACCESS ->
          skipped(
              rule,
              "Missing upstream signal: user API does not expose login or device-change events.");
      case PRESCRIPTION_REJECTED_REPEAT -> evaluateRepeatedRejectedPrescriptions(rule, context);
      case PRESCRIPTION_REUSED -> evaluatePrescriptionReused(rule, context);
      case PRESCRIPTION_MANY_USERS -> evaluatePrescriptionManyUsers(rule, context);
      case PRODUCT_CONTROLLED_QUANTITY -> evaluateControlledQuantity(rule, context);
      case PRODUCT_UNUSUAL_COMBINATION -> evaluateUnusualCombination(rule, context);
    };
  }

  private RuleOutcome evaluateHighQuantity(FraudRule rule, FraudContext context) {
    int totalQuantity =
        safeItems(context.order()).stream()
            .mapToInt(item -> safeQuantity(item.getQuantity()))
            .sum();
    int maxLineQuantity =
        safeItems(context.order()).stream()
            .mapToInt(item -> safeQuantity(item.getQuantity()))
            .max()
            .orElse(0);
    boolean triggered =
        totalQuantity >= HIGH_TOTAL_QUANTITY || maxLineQuantity >= HIGH_LINE_QUANTITY;
    String details =
        "Order quantity evidence: totalUnits=%d, maxLineUnits=%d, thresholds total>=%d or line>=%d."
            .formatted(totalQuantity, maxLineQuantity, HIGH_TOTAL_QUANTITY, HIGH_LINE_QUANTITY);
    return outcome(rule, triggered, details);
  }

  private RuleOutcome evaluateOrderVelocity(FraudRule rule, FraudContext context) {
    LocalDateTime createdAt = context.order().getCreatedAt();
    if (createdAt == null) {
      return skipped(rule, "Missing upstream signal: order createdAt is unavailable.");
    }
    long ordersInHour =
        context.userOrders().stream()
            .filter(order -> order.getCreatedAt() != null)
            .filter(
                order ->
                    Math.abs(Duration.between(order.getCreatedAt(), createdAt).toMinutes()) <= 60)
            .count();
    String details =
        "Order velocity evidence: ordersWithinOneHour=%d, threshold=%d."
            .formatted(ordersInHour, ORDER_VELOCITY_THRESHOLD);
    return outcome(rule, ordersInHour >= ORDER_VELOCITY_THRESHOLD, details);
  }

  private RuleOutcome evaluateRepeatedFailedPayments(FraudRule rule, FraudContext context) {
    LocalDateTime createdAt = context.order().getCreatedAt();
    if (createdAt == null) {
      return skipped(rule, "Missing upstream signal: order createdAt is unavailable.");
    }
    long failedPayments =
        context.userOrders().stream()
            .filter(order -> isWithinDays(order.getCreatedAt(), createdAt, 1))
            .filter(order -> order.getPayment() != null)
            .filter(order -> equalsIgnoreCase(order.getPayment().getStatus(), "FAILED"))
            .count();
    String details =
        "Payment evidence: failedPaymentsInLast24h=%d, threshold=%d. Card fingerprint is not available."
            .formatted(failedPayments, FAILED_PAYMENT_THRESHOLD);
    return outcome(rule, failedPayments >= FAILED_PAYMENT_THRESHOLD, details);
  }

  private RuleOutcome evaluateRestrictedProductFrequency(FraudRule rule, FraudContext context) {
    LocalDateTime createdAt = context.order().getCreatedAt();
    if (createdAt == null) {
      return skipped(rule, "Missing upstream signal: order createdAt is unavailable.");
    }
    Set<Long> restrictedProductIds = restrictedProductIds(context);
    long restrictedOrders =
        context.userOrders().stream()
            .filter(order -> isWithinDays(order.getCreatedAt(), createdAt, 30))
            .filter(
                order ->
                    safeItems(order).stream()
                        .anyMatch(item -> restrictedProductIds.contains(item.getProductId())))
            .count();
    String details =
        "Restricted product evidence: restrictedOrdersInLast30d=%d, threshold=%d."
            .formatted(restrictedOrders, RESTRICTED_PRODUCT_ORDER_THRESHOLD);
    return outcome(rule, restrictedOrders >= RESTRICTED_PRODUCT_ORDER_THRESHOLD, details);
  }

  private RuleOutcome evaluateSharedContact(FraudRule rule, FraudContext context) {
    String address = normalize(context.order().getShippingAddress());
    if (address == null) {
      return skipped(rule, "Missing upstream signal: order shippingAddress is unavailable.");
    }
    long usersAtAddress =
        context.allOrders().stream()
            .filter(order -> address.equals(normalize(order.getShippingAddress())))
            .map(ExternalOrderDto::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
    String details =
        "Shared contact evidence: distinctUsersAtShippingAddress=%d, threshold=%d. Missing upstream signal: phone and card fingerprints are not available."
            .formatted(usersAtAddress, SHARED_ADDRESS_USER_THRESHOLD);
    return outcome(rule, usersAtAddress >= SHARED_ADDRESS_USER_THRESHOLD, details);
  }

  private RuleOutcome evaluateRepeatedRejectedPrescriptions(FraudRule rule, FraudContext context) {
    LocalDateTime createdAt = context.order().getCreatedAt();
    if (createdAt == null) {
      return skipped(rule, "Missing upstream signal: order createdAt is unavailable.");
    }
    long rejected =
        context.userPrescriptions().stream()
            .filter(
                prescription ->
                    isWithinDays(prescription.getReviewedAt(), createdAt, 30)
                        || isWithinDays(prescription.getUploadedAt(), createdAt, 30))
            .filter(prescription -> equalsIgnoreCase(prescription.getStatus(), "REJECTED"))
            .count();
    String details =
        "Prescription evidence: rejectedPrescriptionsInLast30d=%d, threshold=%d."
            .formatted(rejected, REJECTED_PRESCRIPTION_THRESHOLD);
    return outcome(rule, rejected >= REJECTED_PRESCRIPTION_THRESHOLD, details);
  }

  private RuleOutcome evaluatePrescriptionReused(FraudRule rule, FraudContext context) {
    Optional<ExternalPrescriptionDto> current = currentPrescription(context);
    if (current.isEmpty() || normalize(current.get().getImageUrl()) == null) {
      return skipped(
          rule, "Missing upstream signal: order has no prescription imageUrl to compare.");
    }
    String documentKey = normalize(current.get().getImageUrl());
    long reuseCount =
        context.allPrescriptions().stream()
            .filter(prescription -> documentKey.equals(normalize(prescription.getImageUrl())))
            .count();
    String details =
        "Prescription reuse evidence: sameImageUrlCount=%d, threshold=%d. imageUrl is a temporary document identity until documentFingerprint exists."
            .formatted(reuseCount, PRESCRIPTION_REUSE_THRESHOLD);
    return outcome(rule, reuseCount >= PRESCRIPTION_REUSE_THRESHOLD, details);
  }

  private RuleOutcome evaluatePrescriptionManyUsers(FraudRule rule, FraudContext context) {
    Optional<ExternalPrescriptionDto> current = currentPrescription(context);
    if (current.isEmpty() || normalize(current.get().getImageUrl()) == null) {
      return skipped(
          rule, "Missing upstream signal: order has no prescription imageUrl to compare.");
    }
    String documentKey = normalize(current.get().getImageUrl());
    long distinctUsers =
        context.allPrescriptions().stream()
            .filter(prescription -> documentKey.equals(normalize(prescription.getImageUrl())))
            .map(ExternalPrescriptionDto::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
    String details =
        "Prescription cross-user evidence: distinctUsersForSameImageUrl=%d, threshold=%d. imageUrl is a temporary document identity until documentFingerprint exists."
            .formatted(distinctUsers, PRESCRIPTION_REUSE_THRESHOLD);
    return outcome(rule, distinctUsers >= PRESCRIPTION_REUSE_THRESHOLD, details);
  }

  private RuleOutcome evaluateControlledQuantity(FraudRule rule, FraudContext context) {
    int sensitiveUnits =
        safeItems(context.order()).stream()
            .filter(item -> isSensitive(context.productsById().get(item.getProductId())))
            .mapToInt(item -> safeQuantity(item.getQuantity()))
            .sum();
    String details =
        "Product risk evidence: sensitiveUnits=%d, threshold=%d. Sensitivity is inferred from existing product prescription/category/substance fields."
            .formatted(sensitiveUnits, CONTROLLED_QUANTITY_THRESHOLD);
    return outcome(rule, sensitiveUnits >= CONTROLLED_QUANTITY_THRESHOLD, details);
  }

  private RuleOutcome evaluateUnusualCombination(FraudRule rule, FraudContext context) {
    Set<String> riskTags = new HashSet<>();
    for (ExternalOrderItemDto item : safeItems(context.order())) {
      riskTags.addAll(riskTags(context.productsById().get(item.getProductId())));
    }

    boolean triggered =
        riskTags.contains("ANTICOAGULANT") && riskTags.contains("NSAID")
            || riskTags.contains("ANTIBIOTIC") && antibioticLineCount(context) >= 2
            || riskTags.contains("CONTROLLED") && riskTags.contains("SEDATIVE")
            || riskTags.contains("CONTROLLED") && riskTags.contains("OPIOID");
    String details =
        "Product combination evidence: inferredRiskTags=%s. Explicit product riskTags are not available."
            .formatted(riskTags);
    return outcome(rule, triggered, details);
  }

  private int antibioticLineCount(FraudContext context) {
    int count = 0;
    for (ExternalOrderItemDto item : safeItems(context.order())) {
      if (riskTags(context.productsById().get(item.getProductId())).contains("ANTIBIOTIC")) {
        count++;
      }
    }
    return count;
  }

  private Optional<ExternalPrescriptionDto> currentPrescription(FraudContext context) {
    Long prescriptionId = context.order().getPrescriptionId();
    if (prescriptionId == null) {
      return Optional.empty();
    }
    return context.allPrescriptions().stream()
        .filter(prescription -> prescriptionId.equals(prescription.getId()))
        .findFirst();
  }

  private Set<Long> restrictedProductIds(FraudContext context) {
    Set<Long> productIds = new HashSet<>();
    for (Map.Entry<Long, ExternalProductDto> entry : context.productsById().entrySet()) {
      if (isSensitive(entry.getValue())) {
        productIds.add(entry.getKey());
      }
    }
    return productIds;
  }

  private boolean isSensitive(ExternalProductDto product) {
    return product != null
        && (Boolean.TRUE.equals(product.getRequiresPrescription())
            || riskTags(product).contains("CONTROLLED")
            || riskTags(product).contains("ANTIBIOTIC")
            || riskTags(product).contains("ANTICOAGULANT"));
  }

  private Set<String> riskTags(ExternalProductDto product) {
    Set<String> tags = new HashSet<>();
    if (product == null) {
      return tags;
    }
    addTags(tags, product.getName());
    addTags(tags, product.getBrandName());
    addTags(tags, product.getDescription());
    addTags(tags, product.getProductType());
    ExternalCategoryDto category = product.getCategory();
    if (category != null) {
      addTags(tags, category.getName());
      addTags(tags, category.getDescription());
    }
    for (ExternalSubstanceDto substance :
        product.getSubstances() == null
            ? List.<ExternalSubstanceDto>of()
            : product.getSubstances()) {
      addTags(tags, substance.getInn());
      addTags(tags, substance.getCommonName());
      addTags(tags, substance.getAtcCode());
      addTags(tags, substance.getDescription());
    }
    if (Boolean.TRUE.equals(product.getRequiresPrescription())) {
      tags.add("CONTROLLED");
    }
    return tags;
  }

  private void addTags(Set<String> tags, String value) {
    String text = normalize(value);
    if (text == null) {
      return;
    }
    if (text.contains("warfarin") || text.contains("varfarin") || text.contains("anticoagul")) {
      tags.add("ANTICOAGULANT");
    }
    if (text.contains("ibuprofen") || text.contains("brufen") || text.contains("nsaid")) {
      tags.add("NSAID");
    }
    if (text.contains("amoxicillin")
        || text.contains("amoksicilin")
        || text.contains("antibiotic")
        || text.contains("antibiotik")) {
      tags.add("ANTIBIOTIC");
    }
    if (text.contains("opioid") || text.contains("morphine") || text.contains("fentanyl")) {
      tags.add("OPIOID");
    }
    if (text.contains("sedative") || text.contains("benzodiazepine") || text.contains("diazepam")) {
      tags.add("SEDATIVE");
    }
    if (text.contains("controlled") || text.contains("kontrol")) {
      tags.add("CONTROLLED");
    }
  }

  private RuleOutcome outcome(FraudRule rule, boolean triggered, String details) {
    return triggered ? triggered(rule, details) : cleared(rule, details);
  }

  private RuleOutcome triggered(FraudRule rule, String details) {
    return new RuleOutcome(rule, FraudEventType.TRIGGERED, details, safeWeight(rule));
  }

  private RuleOutcome cleared(FraudRule rule, String details) {
    return new RuleOutcome(rule, FraudEventType.CLEARED, details, 0.0);
  }

  private RuleOutcome skipped(FraudRule rule, String details) {
    return new RuleOutcome(rule, FraudEventType.SKIPPED, details, 0.0);
  }

  private double safeWeight(FraudRule rule) {
    return rule.getWeight() == null ? 0.0 : rule.getWeight();
  }

  private List<ExternalOrderItemDto> safeItems(ExternalOrderDto order) {
    return order.getOrderItems() == null ? List.of() : order.getOrderItems();
  }

  private int safeQuantity(Integer quantity) {
    return quantity == null ? 0 : quantity;
  }

  private boolean isWithinDays(LocalDateTime candidate, LocalDateTime anchor, int days) {
    if (candidate == null || anchor == null) {
      return false;
    }
    long minutes = Math.abs(Duration.between(candidate, anchor).toMinutes());
    return minutes <= Duration.ofDays(days).toMinutes();
  }

  private boolean equalsIgnoreCase(String left, String right) {
    return left != null && left.equalsIgnoreCase(right);
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private record FraudContext(
      ExternalOrderDto order,
      List<ExternalOrderDto> userOrders,
      List<ExternalOrderDto> allOrders,
      List<ExternalPrescriptionDto> userPrescriptions,
      List<ExternalPrescriptionDto> allPrescriptions,
      Map<Long, ExternalProductDto> productsById) {}

  public record FraudEvaluation(Long userId, double riskScore, List<RuleOutcome> outcomes) {}

  public record RuleOutcome(
      FraudRule rule, FraudEventType eventType, String details, double scoreContribution) {}
}

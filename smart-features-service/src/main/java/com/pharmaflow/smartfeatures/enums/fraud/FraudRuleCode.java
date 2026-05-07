package com.pharmaflow.smartfeatures.enums.fraud;

import java.util.Arrays;
import java.util.Optional;

public enum FraudRuleCode {
  ORDER_HIGH_QUANTITY("Order high quantity", "ORDER", 20.0),
  ORDER_VELOCITY("Too many orders in short time", "ORDER", 20.0),
  PAYMENT_REPEATED_FAILURES("Repeated failed payments", "ORDER", 25.0),
  USER_RESTRICTED_PRODUCT_FREQUENCY("Restricted products too often", "ORDER", 20.0),
  ACCOUNT_SHARED_CONTACT("Shared account contact signals", "ACCOUNT", 15.0),
  ACCOUNT_NEW_LARGE_ORDER("New account large order", "ACCOUNT", 20.0),
  ACCOUNT_SUSPICIOUS_ACCESS("Suspicious login or device changes", "ACCOUNT", 20.0),
  PRESCRIPTION_REJECTED_REPEAT("Repeated rejected prescriptions", "PRESCRIPTION", 20.0),
  PRESCRIPTION_REUSED("Prescription reused", "PRESCRIPTION", 25.0),
  PRESCRIPTION_MANY_USERS("Prescription used by many users", "PRESCRIPTION", 25.0),
  PRODUCT_CONTROLLED_QUANTITY("Controlled or sensitive product quantity", "PRODUCT", 25.0),
  PRODUCT_UNUSUAL_COMBINATION("Unusual product combination", "PRODUCT", 20.0);

  private final String defaultRuleName;
  private final String category;
  private final double defaultWeight;

  FraudRuleCode(String defaultRuleName, String category, double defaultWeight) {
    this.defaultRuleName = defaultRuleName;
    this.category = category;
    this.defaultWeight = defaultWeight;
  }

  public String getDefaultRuleName() {
    return defaultRuleName;
  }

  public String getCategory() {
    return category;
  }

  public double getDefaultWeight() {
    return defaultWeight;
  }

  public static Optional<FraudRuleCode> fromCode(String value) {
    if (value == null) {
      return Optional.empty();
    }
    String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase();
    return Arrays.stream(values()).filter(code -> code.name().equals(normalized)).findFirst();
  }
}

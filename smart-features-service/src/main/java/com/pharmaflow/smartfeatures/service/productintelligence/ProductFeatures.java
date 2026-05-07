package com.pharmaflow.smartfeatures.service.productintelligence;

import java.util.Set;

public record ProductFeatures(
    Set<String> tokens,
    Set<String> categoryTokens,
    Set<String> substanceTokens,
    String productType,
    boolean requiresPrescription,
    String embeddingText) {}

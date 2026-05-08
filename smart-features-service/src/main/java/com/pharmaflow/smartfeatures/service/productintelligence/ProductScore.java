package com.pharmaflow.smartfeatures.service.productintelligence;

import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import java.util.List;

public record ProductScore(
    ProductSnapshot product, double score, String reason, List<Long> matchedSymptomIds) {}

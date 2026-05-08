package com.pharmaflow.smartfeatures.service.productintelligence;

import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import com.pharmaflow.smartfeatures.service.chatbot.EmbeddingService;
import java.util.List;
import java.util.OptionalDouble;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingSimilarityService {

  private final EmbeddingService embeddingService;

  public EmbeddingSimilarityService(EmbeddingService embeddingService) {
    this.embeddingService = embeddingService;
  }

  public OptionalDouble similarity(String left, String right) {
    if (!embeddingService.isEnabled()) {
      return OptionalDouble.empty();
    }
    try {
      return OptionalDouble.of(cosine(embeddingService.embed(left), embeddingService.embed(right)));
    } catch (BadRequestException | ExternalServiceException ex) {
      return OptionalDouble.empty();
    }
  }

  private double cosine(List<Double> left, List<Double> right) {
    if (left.size() != right.size() || left.isEmpty()) {
      return 0.0;
    }
    double dot = 0.0;
    double leftNorm = 0.0;
    double rightNorm = 0.0;
    for (int i = 0; i < left.size(); i++) {
      double leftValue = left.get(i);
      double rightValue = right.get(i);
      dot += leftValue * rightValue;
      leftNorm += leftValue * leftValue;
      rightNorm += rightValue * rightValue;
    }
    if (leftNorm == 0.0 || rightNorm == 0.0) {
      return 0.0;
    }
    return Math.max(0.0, Math.min(1.0, dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm))));
  }
}

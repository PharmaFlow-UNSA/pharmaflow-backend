package com.pharmaflow.smartfeatures.service.chatbot;

import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the embedding service and returns normalized 384-dimensional vectors for semantic FAQ
 * search.
 */
@Service
public class EmbeddingService {

  private static final int EXPECTED_DIMENSIONS = 384;

  private final RestClient restClient;
  private final boolean enabled;

  public EmbeddingService(
      @Value("${smartfeatures.embedding-service.url:http://localhost:8000}")
          String embeddingServiceUrl,
      @Value("${smartfeatures.embedding.enabled:true}") boolean enabled) {
    this.restClient = RestClient.builder().baseUrl(embeddingServiceUrl).build();
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public List<Double> embed(String text) {
    if (!enabled) {
      throw new ExternalServiceException("Embedding service integration is disabled.");
    }

    String sanitizedText = TextSanitizer.sanitizeRequiredText(text);
    if (sanitizedText == null) {
      throw new BadRequestException("text is required for embedding.");
    }

    try {
      EmbedResponse response =
          restClient
              .post()
              .uri("/embed")
              .body(new EmbedRequest(List.of(sanitizedText)))
              .retrieve()
              .body(EmbedResponse.class);

      if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
        throw new ExternalServiceException("Embedding service returned an empty response.");
      }

      List<Double> embedding = response.embeddings().get(0);
      if (embedding == null || embedding.size() != EXPECTED_DIMENSIONS) {
        throw new ExternalServiceException(
            "Embedding service returned a vector with invalid dimensions.");
      }
      return embedding;
    } catch (RestClientException ex) {
      throw new ExternalServiceException("Embedding service is unavailable.");
    }
  }

  private record EmbedRequest(List<String> texts) {}

  private record EmbedResponse(List<List<Double>> embeddings) {}
}

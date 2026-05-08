package com.pharmaflow.smartfeatures.service.chatbot;

import com.pharmaflow.smartfeatures.dto.chatbot.ChatbotAskRequestDto;
import com.pharmaflow.smartfeatures.dto.chatbot.ChatbotAskResponseDto;
import com.pharmaflow.smartfeatures.service.chatbot.FaqEmbeddingRepository.FaqEmbeddingCandidate;
import com.pharmaflow.smartfeatures.service.chatbot.FaqEmbeddingRepository.FaqSemanticMatch;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatbotService {

  public static final String FALLBACK_ANSWER =
      "I'm not sure I found the right answer. Please rephrase your question or contact support/pharmacist. "
          + "For medical advice, please contact a licensed pharmacist.";

  private final EmbeddingService embeddingService;
  private final FaqEmbeddingRepository faqEmbeddingRepository;
  private final double confidenceThreshold;

  public ChatbotService(
      EmbeddingService embeddingService,
      FaqEmbeddingRepository faqEmbeddingRepository,
      @Value("${smartfeatures.chatbot.confidence-threshold:0.75}") double confidenceThreshold) {
    this.embeddingService = embeddingService;
    this.faqEmbeddingRepository = faqEmbeddingRepository;
    this.confidenceThreshold = confidenceThreshold;
  }

  @Transactional
  public ChatbotAskResponseDto ask(ChatbotAskRequestDto requestDto) {
    String message = TextSanitizer.sanitizeRequiredText(requestDto.getMessage());
    backfillMissingFaqEmbeddings();

    List<Double> queryEmbedding = embeddingService.embed(message);
    return faqEmbeddingRepository
        .findBestActiveMatch(queryEmbedding)
        .filter(match -> match.similarity() >= confidenceThreshold)
        .map(this::toMatchedResponse)
        .orElseGet(this::fallbackResponse);
  }

  private void backfillMissingFaqEmbeddings() {
    for (FaqEmbeddingCandidate candidate :
        faqEmbeddingRepository.findActiveEntriesMissingEmbedding()) {
      faqEmbeddingRepository.updateEmbedding(
          candidate.faqId(), embeddingService.embed(candidate.question()));
    }
  }

  private ChatbotAskResponseDto toMatchedResponse(FaqSemanticMatch match) {
    return new ChatbotAskResponseDto(
        match.answer(), match.question(), match.similarity(), match.category(), false);
  }

  private ChatbotAskResponseDto fallbackResponse() {
    return new ChatbotAskResponseDto(FALLBACK_ANSWER, null, 0.0, null, true);
  }
}

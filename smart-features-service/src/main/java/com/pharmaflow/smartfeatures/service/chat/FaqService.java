package com.pharmaflow.smartfeatures.service.chat;

import com.pharmaflow.smartfeatures.dto.chat.FaqEntryRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryResponseDto;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.chat.FaqMapper;
import com.pharmaflow.smartfeatures.model.chat.FaqEntry;
import com.pharmaflow.smartfeatures.repositories.chat.FaqEntryRepository;
import com.pharmaflow.smartfeatures.service.chatbot.EmbeddingService;
import com.pharmaflow.smartfeatures.service.chatbot.FaqEmbeddingRepository;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages FAQ entries and refreshes their semantic-search embeddings on create/update. */
@Service
public class FaqService {

  private final FaqEntryRepository faqEntryRepository;
  private final FaqMapper faqMapper;
  private final EmbeddingService embeddingService;
  private final FaqEmbeddingRepository faqEmbeddingRepository;

  public FaqService(
      FaqEntryRepository faqEntryRepository,
      FaqMapper faqMapper,
      EmbeddingService embeddingService,
      FaqEmbeddingRepository faqEmbeddingRepository) {
    this.faqEntryRepository = faqEntryRepository;
    this.faqMapper = faqMapper;
    this.embeddingService = embeddingService;
    this.faqEmbeddingRepository = faqEmbeddingRepository;
  }

  @Transactional(readOnly = true)
  public List<FaqEntryResponseDto> getFaqEntries() {
    return faqEntryRepository.findAllByOrderByQuestionAsc().stream()
        .map(faqMapper::toResponseDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public FaqEntryResponseDto getFaqEntry(Long id) {
    return faqMapper.toResponseDto(findFaqEntryById(id));
  }

  @Transactional(readOnly = true)
  public List<FaqEntryResponseDto> searchFaqEntries(String query) {
    String sanitizedQuery = sanitizeRequired(query, "query");
    if (sanitizedQuery.length() < 2) {
      throw new BadRequestException("query must contain at least 2 characters.");
    }
    return faqEntryRepository.searchActive(sanitizedQuery).stream()
        .map(faqMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public FaqEntryResponseDto createFaqEntry(FaqEntryRequestDto requestDto) {
    String normalizedQuestion = TextSanitizer.normalizeKey(requestDto.getQuestion());
    ensureQuestionUnique(normalizedQuestion, null);

    FaqEntry faqEntry = faqMapper.toEntity(requestDto);
    faqEntry.setQuestion(sanitizeRequired(requestDto.getQuestion(), "question"));
    faqEntry.setAnswer(sanitizeRequired(requestDto.getAnswer(), "answer"));
    faqEntry.setKeywords(TextSanitizer.sanitizeOptionalText(requestDto.getKeywords()));
    faqEntry.setUpdatedAt(LocalDateTime.now());

    FaqEntry savedFaqEntry = faqEntryRepository.save(faqEntry);
    updateEmbeddingIfEnabled(savedFaqEntry);
    return faqMapper.toResponseDto(savedFaqEntry);
  }

  @Transactional
  public FaqEntryResponseDto updateFaqEntry(Long id, FaqEntryRequestDto requestDto) {
    FaqEntry faqEntry = findFaqEntryById(id);
    String normalizedQuestion = TextSanitizer.normalizeKey(requestDto.getQuestion());
    ensureQuestionUnique(normalizedQuestion, id);

    faqMapper.updateEntity(requestDto, faqEntry);
    faqEntry.setQuestion(sanitizeRequired(requestDto.getQuestion(), "question"));
    faqEntry.setAnswer(sanitizeRequired(requestDto.getAnswer(), "answer"));
    faqEntry.setKeywords(TextSanitizer.sanitizeOptionalText(requestDto.getKeywords()));
    faqEntry.setUpdatedAt(LocalDateTime.now());

    FaqEntry savedFaqEntry = faqEntryRepository.save(faqEntry);
    updateEmbeddingIfEnabled(savedFaqEntry);
    return faqMapper.toResponseDto(savedFaqEntry);
  }

  @Transactional
  public void deleteFaqEntry(Long id) {
    faqEntryRepository.delete(findFaqEntryById(id));
  }

  private FaqEntry findFaqEntryById(Long id) {
    return faqEntryRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FAQ entry not found with id: " + id));
  }

  private void ensureQuestionUnique(String normalizedQuestion, Long currentId) {
    boolean duplicateExists =
        currentId == null
            ? faqEntryRepository.existsByNormalizedQuestion(normalizedQuestion)
            : faqEntryRepository.existsByNormalizedQuestionAndFaqIdNot(
                normalizedQuestion, currentId);

    if (duplicateExists) {
      throw new DuplicateResourceException("FAQ entry with the same question already exists.");
    }
  }

  private String sanitizeRequired(String value, String fieldName) {
    String sanitized = TextSanitizer.sanitizeRequiredText(value);
    if (sanitized == null) {
      throw new BadRequestException(fieldName + " is required.");
    }
    return sanitized;
  }

  private void updateEmbeddingIfEnabled(FaqEntry faqEntry) {
    if (embeddingService.isEnabled()) {
      faqEmbeddingRepository.updateEmbedding(
          faqEntry.getFaqId(), embeddingService.embed(faqEntry.getQuestion()));
    }
  }
}

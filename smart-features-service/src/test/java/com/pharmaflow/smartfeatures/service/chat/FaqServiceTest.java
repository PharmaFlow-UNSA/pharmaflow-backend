package com.pharmaflow.smartfeatures.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryResponseDto;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.mapper.chat.FaqMapper;
import com.pharmaflow.smartfeatures.model.chat.FaqEntry;
import com.pharmaflow.smartfeatures.repositories.chat.FaqEntryRepository;
import com.pharmaflow.smartfeatures.service.chatbot.EmbeddingService;
import com.pharmaflow.smartfeatures.service.chatbot.FaqEmbeddingRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

  @Mock private FaqEntryRepository faqEntryRepository;

  @Mock private EmbeddingService embeddingService;

  @Mock private FaqEmbeddingRepository faqEmbeddingRepository;

  private FaqService faqService;

  @BeforeEach
  void setUp() {
    faqService =
        new FaqService(
            faqEntryRepository,
            new FaqMapper(new ModelMapperConfig().modelMapper()),
            embeddingService,
            faqEmbeddingRepository);
  }

  @Test
  void createFaqEntryShouldTrimFieldsAndSetUpdatedAt() {
    FaqEntryRequestDto requestDto =
        new FaqEntryRequestDto(
            "  How to pay?  ", "  Use card or cash.  ", FaqCategory.PAYMENTS, "  pay  ", true);

    when(faqEntryRepository.existsByNormalizedQuestion("how to pay?")).thenReturn(false);
    when(embeddingService.isEnabled()).thenReturn(false);
    when(faqEntryRepository.save(any(FaqEntry.class)))
        .thenAnswer(
            invocation -> {
              FaqEntry faqEntry = invocation.getArgument(0);
              faqEntry.setFaqId(1L);
              return faqEntry;
            });

    FaqEntryResponseDto response = faqService.createFaqEntry(requestDto);

    ArgumentCaptor<FaqEntry> captor = ArgumentCaptor.forClass(FaqEntry.class);
    verify(faqEntryRepository).save(captor.capture());
    assertThat(captor.getValue().getQuestion()).isEqualTo("How to pay?");
    assertThat(captor.getValue().getAnswer()).isEqualTo("Use card or cash.");
    assertThat(captor.getValue().getKeywords()).isEqualTo("pay");
    assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getQuestion()).isEqualTo("How to pay?");
    assertThat(response.getActive()).isTrue();
  }

  @Test
  void createFaqEntryShouldStoreEmbeddingWhenEnabled() {
    FaqEntryRequestDto requestDto =
        new FaqEntryRequestDto(
            "How to pay?", "Use card or cash.", FaqCategory.PAYMENTS, "pay", true);

    when(faqEntryRepository.existsByNormalizedQuestion("how to pay?")).thenReturn(false);
    when(embeddingService.isEnabled()).thenReturn(true);
    when(embeddingService.embed("How to pay?")).thenReturn(Collections.nCopies(384, 0.01));
    when(faqEntryRepository.save(any(FaqEntry.class)))
        .thenAnswer(
            invocation -> {
              FaqEntry faqEntry = invocation.getArgument(0);
              faqEntry.setFaqId(10L);
              return faqEntry;
            });

    faqService.createFaqEntry(requestDto);

    verify(faqEmbeddingRepository).updateEmbedding(10L, Collections.nCopies(384, 0.01));
  }

  @Test
  void createFaqEntryShouldRejectDuplicateQuestion() {
    FaqEntryRequestDto requestDto =
        new FaqEntryRequestDto(
            "How to pay?", "Use card or cash.", FaqCategory.PAYMENTS, null, true);
    when(faqEntryRepository.existsByNormalizedQuestion("how to pay?")).thenReturn(true);

    assertThatThrownBy(() -> faqService.createFaqEntry(requestDto))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessage("FAQ entry with the same question already exists.");

    verify(faqEntryRepository, never()).save(any(FaqEntry.class));
  }

  @Test
  void searchFaqEntriesShouldRejectTooShortQuery() {
    assertThatThrownBy(() -> faqService.searchFaqEntries(" a "))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("query must contain at least 2 characters.");

    verify(faqEntryRepository, never()).searchActive(any());
  }

  @Test
  void searchFaqEntriesShouldUseTrimmedQuery() {
    FaqEntry faqEntry =
        FaqEntry.builder()
            .faqId(7L)
            .question("How to pay?")
            .answer("Use card or cash.")
            .category(FaqCategory.PAYMENTS)
            .isActive(true)
            .build();
    when(faqEntryRepository.searchActive("pay")).thenReturn(List.of(faqEntry));

    List<FaqEntryResponseDto> response = faqService.searchFaqEntries("  pay  ");

    assertThat(response).hasSize(1);
    assertThat(response.get(0).getQuestion()).isEqualTo("How to pay?");
    verify(faqEntryRepository).searchActive("pay");
  }
}

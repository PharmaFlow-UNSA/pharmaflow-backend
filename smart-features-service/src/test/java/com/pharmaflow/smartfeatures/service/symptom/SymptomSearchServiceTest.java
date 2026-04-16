package com.pharmaflow.smartfeatures.service.symptom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchResponseDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchRequestDto;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.mapper.symptom.SymptomProductMatchMapper;
import com.pharmaflow.smartfeatures.mapper.symptom.SymptomSearchMapper;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearchItem;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomProductMatchRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchItemRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SymptomSearchServiceTest {

    @Mock
    private SymptomSearchRepository symptomSearchRepository;

    @Mock
    private SymptomSearchItemRepository symptomSearchItemRepository;

    @Mock
    private SymptomProductMatchRepository symptomProductMatchRepository;

    @Mock
    private SymptomRepository symptomRepository;

    private SymptomSearchService symptomSearchService;

    @BeforeEach
    void setUp() {
        ModelMapperConfig modelMapperConfig = new ModelMapperConfig();
        symptomSearchService = new SymptomSearchService(
                symptomSearchRepository,
                symptomSearchItemRepository,
                symptomProductMatchRepository,
                symptomRepository,
                new SymptomSearchMapper(modelMapperConfig.modelMapper()),
                new SymptomProductMatchMapper(modelMapperConfig.modelMapper()));
    }

    @Test
    void createSearchShouldTrimQueryAndSetTimestamp() {
        when(symptomSearchRepository.save(any(SymptomSearch.class))).thenAnswer(invocation -> {
            SymptomSearch search = invocation.getArgument(0);
            search.setSearchId(4L);
            return search;
        });

        SymptomSearchResponseDto response =
                symptomSearchService.createSearch(new SymptomSearchRequestDto(10L, 11L, "  dry cough  "));

        ArgumentCaptor<SymptomSearch> captor = ArgumentCaptor.forClass(SymptomSearch.class);
        verify(symptomSearchRepository).save(captor.capture());
        assertThat(captor.getValue().getSearchQuery()).isEqualTo("dry cough");
        assertThat(captor.getValue().getSearchedAt()).isNotNull();
        assertThat(response.getId()).isEqualTo(4L);
    }

    @Test
    void addItemShouldRejectDuplicateSymptom() {
        SymptomSearch search = SymptomSearch.builder().searchId(1L).userId(10L).searchQuery("fever").build();
        Symptom symptom = Symptom.builder()
                .symptomId(2L)
                .name("Fever")
                .severityLevel(SymptomSeverityLevel.HIGH)
                .isActive(true)
                .build();
        when(symptomSearchRepository.findById(1L)).thenReturn(Optional.of(search));
        when(symptomRepository.findById(2L)).thenReturn(Optional.of(symptom));
        when(symptomSearchItemRepository.existsBySearchSearchIdAndSymptomSymptomId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> symptomSearchService.addItem(1L, new SymptomSearchItemRequestDto(2L)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Symptom is already linked to this search.");

        verify(symptomSearchItemRepository, never()).save(any(SymptomSearchItem.class));
    }

    @Test
    void getMatchesShouldDeduplicateSymptomsAndSortByRelevance() {
        Symptom dryCough = Symptom.builder().symptomId(1L).name("Dry Cough").isActive(true).build();
        Symptom fever = Symptom.builder().symptomId(2L).name("Fever").isActive(true).build();
        SymptomSearch search = SymptomSearch.builder().searchId(7L).userId(10L).searchQuery("dry cough").build();

        when(symptomSearchRepository.findById(7L)).thenReturn(Optional.of(search));
        when(symptomSearchItemRepository.findBySearchSearchIdOrderBySearchItemIdAsc(7L))
                .thenReturn(List.of(
                        SymptomSearchItem.builder().searchItemId(1L).search(search).symptom(dryCough).build(),
                        SymptomSearchItem.builder().searchItemId(2L).search(search).symptom(dryCough).build(),
                        SymptomSearchItem.builder().searchItemId(3L).search(search).symptom(fever).build()));
        when(symptomProductMatchRepository.findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(1L))
                .thenReturn(List.of(
                        SymptomProductMatch.builder()
                                .matchId(10L)
                                .symptom(dryCough)
                                .productId(100L)
                                .relevanceScore(0.9)
                                .build()));
        when(symptomProductMatchRepository.findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(2L))
                .thenReturn(List.of(
                        SymptomProductMatch.builder()
                                .matchId(20L)
                                .symptom(fever)
                                .productId(200L)
                                .relevanceScore(0.7)
                                .build()));

        List<SymptomProductMatchResponseDto> matches = symptomSearchService.getMatches(7L);

        assertThat(matches).hasSize(2);
        assertThat(matches).extracting(SymptomProductMatchResponseDto::getProductId).containsExactly(100L, 200L);
        verify(symptomProductMatchRepository).findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(1L);
        verify(symptomProductMatchRepository).findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(2L);
    }
}

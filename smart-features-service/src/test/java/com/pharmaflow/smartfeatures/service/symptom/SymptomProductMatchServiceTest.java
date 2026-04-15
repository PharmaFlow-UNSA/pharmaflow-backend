package com.pharmaflow.smartfeatures.service.symptom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.mapper.symptom.SymptomProductMatchMapper;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomProductMatchRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SymptomProductMatchServiceTest {

    @Mock
    private SymptomProductMatchRepository symptomProductMatchRepository;

    @Mock
    private SymptomRepository symptomRepository;

    private SymptomProductMatchService symptomProductMatchService;

    @BeforeEach
    void setUp() {
        symptomProductMatchService = new SymptomProductMatchService(
                symptomProductMatchRepository,
                symptomRepository,
                new SymptomProductMatchMapper(new ModelMapperConfig().modelMapper()));
    }

    @Test
    void createMatchShouldRejectDuplicateProduct() {
        Symptom symptom = Symptom.builder()
                .symptomId(1L)
                .name("Dry Cough")
                .severityLevel(SymptomSeverityLevel.MEDIUM)
                .isActive(true)
                .build();
        when(symptomRepository.findById(1L)).thenReturn(Optional.of(symptom));
        when(symptomProductMatchRepository.existsBySymptomSymptomIdAndProductId(1L, 300L)).thenReturn(true);

        assertThatThrownBy(() -> symptomProductMatchService.createMatch(
                        1L, new SymptomProductMatchRequestDto(300L, 0.9, "Helpful")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Product is already matched to this symptom.");

        verify(symptomProductMatchRepository, never()).save(any(SymptomProductMatch.class));
    }

    @Test
    void updateMatchShouldTrimMatchReasonAndReturnMappedDto() {
        Symptom symptom = Symptom.builder()
                .symptomId(1L)
                .name("Dry Cough")
                .severityLevel(SymptomSeverityLevel.MEDIUM)
                .isActive(true)
                .build();
        SymptomProductMatch match = SymptomProductMatch.builder()
                .matchId(5L)
                .symptom(symptom)
                .productId(300L)
                .matchReason("Old")
                .build();
        when(symptomRepository.findById(1L)).thenReturn(Optional.of(symptom));
        when(symptomProductMatchRepository.findBySymptomSymptomIdAndMatchId(1L, 5L)).thenReturn(Optional.of(match));
        when(symptomProductMatchRepository.existsBySymptomSymptomIdAndProductIdAndMatchIdNot(1L, 301L, 5L))
                .thenReturn(false);
        when(symptomProductMatchRepository.save(match)).thenReturn(match);

        SymptomProductMatchResponseDto response = symptomProductMatchService.updateMatch(
                1L, 5L, new SymptomProductMatchRequestDto(301L, 0.8, "  Night relief  "));

        assertThat(match.getProductId()).isEqualTo(301L);
        assertThat(match.getMatchReason()).isEqualTo("Night relief");
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getSymptomId()).isEqualTo(1L);
    }
}

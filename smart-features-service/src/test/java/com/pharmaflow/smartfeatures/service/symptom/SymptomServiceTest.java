package com.pharmaflow.smartfeatures.service.symptom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomResponseDto;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.symptom.SymptomMapper;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SymptomServiceTest {

  @Mock private SymptomRepository symptomRepository;

  private SymptomMapper symptomMapper;

  @InjectMocks private SymptomService symptomService;

  private Symptom activeSymptom;
  private Symptom inactiveSymptom;
  private SymptomRequestDto requestDto;

  @BeforeEach
  void setUp() {
    symptomMapper = new SymptomMapper(new ModelMapperConfig().modelMapper());
    symptomService = new SymptomService(symptomRepository, symptomMapper);

    activeSymptom =
        Symptom.builder()
            .symptomId(1L)
            .name("Fever")
            .normalizedName("fever")
            .description("Elevated body temperature")
            .severityLevel(SymptomSeverityLevel.HIGH)
            .isActive(true)
            .build();

    inactiveSymptom =
        Symptom.builder()
            .symptomId(2L)
            .name("Sneezing")
            .normalizedName("sneezing")
            .description("Repeated sneezing")
            .severityLevel(SymptomSeverityLevel.LOW)
            .isActive(false)
            .build();

    requestDto =
        new SymptomRequestDto(
            "  Fever  ", "  Elevated body temperature  ", SymptomSeverityLevel.HIGH, true);
  }

  @Test
  void getAllSymptoms_ShouldReturnOnlyActiveSymptomsFromRepository() {
    when(symptomRepository.findAllByIsActiveTrueOrderByNameAsc())
        .thenReturn(List.of(activeSymptom));

    List<SymptomResponseDto> result = symptomService.getAllSymptoms();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Fever");
    assertThat(result.get(0).getActive()).isTrue();
  }

  @Test
  void getSymptomById_ShouldReturnInactiveSymptomWhenItExists() {
    when(symptomRepository.findById(2L)).thenReturn(Optional.of(inactiveSymptom));

    assertThatThrownBy(() -> symptomService.getSymptomById(2L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Symptom not found with id: 2");
  }

  @Test
  void searchSymptoms_ShouldTrimQueryAndReturnMappedResults() {
    when(symptomRepository.searchActiveSymptoms("fever")).thenReturn(List.of(activeSymptom));

    List<SymptomResponseDto> result = symptomService.searchSymptoms("  fever  ");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Fever");
    verify(symptomRepository).searchActiveSymptoms("fever");
  }

  @Test
  void searchSymptoms_WithShortTrimmedQuery_ShouldThrowBadRequest() {
    assertThatThrownBy(() -> symptomService.searchSymptoms(" f "))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must contain at least 2 characters.");

    verify(symptomRepository, never()).searchActiveSymptoms(any());
  }

  @Test
  void searchSymptoms_WithNullQuery_ShouldThrowBadRequest() {
    assertThatThrownBy(() -> symptomService.searchSymptoms(null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must contain at least 2 characters.");

    verify(symptomRepository, never()).searchActiveSymptoms(any());
  }

  @Test
  void createSymptom_ShouldTrimFieldsAndPersistMappedEntity() {
    when(symptomRepository.existsByNormalizedName("fever")).thenReturn(false);
    when(symptomRepository.save(any(Symptom.class)))
        .thenAnswer(
            invocation -> {
              Symptom saved = invocation.getArgument(0);
              saved.setSymptomId(1L);
              return saved;
            });

    SymptomResponseDto result = symptomService.createSymptom(requestDto);

    ArgumentCaptor<Symptom> symptomCaptor = ArgumentCaptor.forClass(Symptom.class);
    verify(symptomRepository).save(symptomCaptor.capture());

    assertThat(symptomCaptor.getValue().getName()).isEqualTo("Fever");
    assertThat(symptomCaptor.getValue().getDescription()).isEqualTo("Elevated body temperature");
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("Fever");
  }

  @Test
  void createSymptom_WhenDuplicateExists_ShouldThrowConflict() {
    when(symptomRepository.existsByNormalizedName("fever")).thenReturn(true);

    assertThatThrownBy(() -> symptomService.createSymptom(requestDto))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessage("Symptom with name 'Fever' already exists.");

    verify(symptomRepository, never()).save(any(Symptom.class));
  }

  @Test
  void createSymptom_WhenSanitizedNameFallsBelowMinimum_ShouldThrowBadRequest() {
    SymptomRequestDto invalidRequest =
        new SymptomRequestDto(" a ", "desc", SymptomSeverityLevel.LOW, true);

    assertThatThrownBy(() -> symptomService.createSymptom(invalidRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Symptom name must be between 2 and 100 characters after trimming.");

    verify(symptomRepository, never()).save(any(Symptom.class));
  }

  @Test
  void updateSymptom_WhenAnotherSymptomHasSameName_ShouldThrowConflict() {
    when(symptomRepository.findById(1L)).thenReturn(Optional.of(activeSymptom));
    when(symptomRepository.existsByNormalizedNameAndSymptomIdNot("fever", 1L)).thenReturn(true);

    assertThatThrownBy(() -> symptomService.updateSymptom(1L, requestDto))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessage("Symptom with name 'Fever' already exists.");
  }

  @Test
  void updateSymptom_WhenInactiveAndStillInactive_ShouldThrowNotFound() {
    SymptomRequestDto inactiveUpdateRequest =
        new SymptomRequestDto("Sneezing", "Still inactive", SymptomSeverityLevel.LOW, false);
    when(symptomRepository.findById(2L)).thenReturn(Optional.of(inactiveSymptom));

    assertThatThrownBy(() -> symptomService.updateSymptom(2L, inactiveUpdateRequest))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Symptom not found with id: 2");

    verify(symptomRepository, never()).save(any(Symptom.class));
  }

  @Test
  void updateSymptom_WhenInactiveAndReactivated_ShouldSucceed() {
    SymptomRequestDto reactivationRequest =
        new SymptomRequestDto("Sneezing", "Back to active", SymptomSeverityLevel.LOW, true);

    when(symptomRepository.findById(2L)).thenReturn(Optional.of(inactiveSymptom));
    when(symptomRepository.existsByNormalizedNameAndSymptomIdNot("sneezing", 2L)).thenReturn(false);
    when(symptomRepository.save(inactiveSymptom)).thenReturn(inactiveSymptom);

    SymptomResponseDto result = symptomService.updateSymptom(2L, reactivationRequest);

    assertThat(inactiveSymptom.isActive()).isTrue();
    assertThat(result.getActive()).isTrue();
  }

  @Test
  void updateSymptom_ShouldAllowFullUpdateIncludingActiveFlag() {
    SymptomRequestDto updateRequest =
        new SymptomRequestDto(
            "  Dry Cough ", " Persistent cough ", SymptomSeverityLevel.MEDIUM, false);

    when(symptomRepository.findById(1L)).thenReturn(Optional.of(activeSymptom));
    when(symptomRepository.existsByNormalizedNameAndSymptomIdNot("dry cough", 1L))
        .thenReturn(false);
    when(symptomRepository.save(activeSymptom)).thenReturn(activeSymptom);

    SymptomResponseDto result = symptomService.updateSymptom(1L, updateRequest);

    assertThat(activeSymptom.getName()).isEqualTo("Dry Cough");
    assertThat(activeSymptom.getDescription()).isEqualTo("Persistent cough");
    assertThat(activeSymptom.isActive()).isFalse();
    assertThat(result.getActive()).isFalse();
  }

  @Test
  void deleteSymptom_WhenMissing_ShouldThrowNotFound() {
    when(symptomRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> symptomService.deleteSymptom(42L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Symptom not found with id: 42");
  }

  @Test
  void deleteSymptom_WhenInactive_ShouldThrowNotFound() {
    when(symptomRepository.findById(2L)).thenReturn(Optional.of(inactiveSymptom));

    assertThatThrownBy(() -> symptomService.deleteSymptom(2L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Symptom not found with id: 2");

    verify(symptomRepository, never()).delete(any(Symptom.class));
  }
}

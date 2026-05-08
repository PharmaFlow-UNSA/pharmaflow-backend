package com.pharmaflow.smartfeatures.service.symptom;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.symptom.SymptomProductMatchMapper;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomProductMatchRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SymptomProductMatchService {

  private final SymptomProductMatchRepository symptomProductMatchRepository;
  private final SymptomRepository symptomRepository;
  private final SymptomProductMatchMapper symptomProductMatchMapper;

  public SymptomProductMatchService(
      SymptomProductMatchRepository symptomProductMatchRepository,
      SymptomRepository symptomRepository,
      SymptomProductMatchMapper symptomProductMatchMapper) {
    this.symptomProductMatchRepository = symptomProductMatchRepository;
    this.symptomRepository = symptomRepository;
    this.symptomProductMatchMapper = symptomProductMatchMapper;
  }

  @Transactional(readOnly = true)
  public List<SymptomProductMatchResponseDto> getMatchesBySymptom(Long symptomId) {
    findActiveSymptomById(symptomId);
    return symptomProductMatchRepository
        .findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(symptomId)
        .stream()
        .map(symptomProductMatchMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public SymptomProductMatchResponseDto createMatch(
      Long symptomId, SymptomProductMatchRequestDto requestDto) {
    Symptom symptom = findActiveSymptomById(symptomId);
    if (symptomProductMatchRepository.existsBySymptomSymptomIdAndProductId(
        symptomId, requestDto.getProductId())) {
      throw new DuplicateResourceException("Product is already matched to this symptom.");
    }

    SymptomProductMatch match = symptomProductMatchMapper.toEntity(requestDto);
    match.setSymptom(symptom);
    match.setMatchReason(TextSanitizer.sanitizeOptionalText(requestDto.getMatchReason()));
    return symptomProductMatchMapper.toResponseDto(symptomProductMatchRepository.save(match));
  }

  @Transactional
  public SymptomProductMatchResponseDto updateMatch(
      Long symptomId, Long matchId, SymptomProductMatchRequestDto requestDto) {
    findActiveSymptomById(symptomId);
    SymptomProductMatch match = findMatch(symptomId, matchId);

    if (symptomProductMatchRepository.existsBySymptomSymptomIdAndProductIdAndMatchIdNot(
        symptomId, requestDto.getProductId(), matchId)) {
      throw new DuplicateResourceException("Product is already matched to this symptom.");
    }

    symptomProductMatchMapper.updateEntity(requestDto, match);
    match.setMatchReason(TextSanitizer.sanitizeOptionalText(requestDto.getMatchReason()));
    return symptomProductMatchMapper.toResponseDto(symptomProductMatchRepository.save(match));
  }

  @Transactional
  public void deleteMatch(Long symptomId, Long matchId) {
    findActiveSymptomById(symptomId);
    symptomProductMatchRepository.delete(findMatch(symptomId, matchId));
  }

  private Symptom findActiveSymptomById(Long id) {
    Symptom symptom =
        symptomRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Symptom not found with id: " + id));
    if (!symptom.isActive()) {
      throw new ResourceNotFoundException("Symptom not found with id: " + id);
    }
    return symptom;
  }

  private SymptomProductMatch findMatch(Long symptomId, Long matchId) {
    return symptomProductMatchRepository
        .findBySymptomSymptomIdAndMatchId(symptomId, matchId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Symptom product match not found with id: "
                        + matchId
                        + " for symptom: "
                        + symptomId));
  }
}

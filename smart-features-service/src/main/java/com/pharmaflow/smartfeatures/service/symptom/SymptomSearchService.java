package com.pharmaflow.smartfeatures.service.symptom;

import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemResponseDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchResponseDto;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
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
import com.pharmaflow.smartfeatures.service.productintelligence.ProductRanker;
import com.pharmaflow.smartfeatures.service.productintelligence.ProductScore;
import com.pharmaflow.smartfeatures.service.productintelligence.UserHealthContext;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SymptomSearchService {

  private final SymptomSearchRepository symptomSearchRepository;
  private final SymptomSearchItemRepository symptomSearchItemRepository;
  private final SymptomProductMatchRepository symptomProductMatchRepository;
  private final SymptomRepository symptomRepository;
  private final SymptomSearchMapper symptomSearchMapper;
  private final SymptomProductMatchMapper symptomProductMatchMapper;
  private final ProductCatalogClient productCatalogClient;
  private final UserHealthClient userHealthClient;
  private final ProductRanker productRanker;

  public SymptomSearchService(
      SymptomSearchRepository symptomSearchRepository,
      SymptomSearchItemRepository symptomSearchItemRepository,
      SymptomProductMatchRepository symptomProductMatchRepository,
      SymptomRepository symptomRepository,
      SymptomSearchMapper symptomSearchMapper,
      SymptomProductMatchMapper symptomProductMatchMapper,
      ProductCatalogClient productCatalogClient,
      UserHealthClient userHealthClient,
      ProductRanker productRanker) {
    this.symptomSearchRepository = symptomSearchRepository;
    this.symptomSearchItemRepository = symptomSearchItemRepository;
    this.symptomProductMatchRepository = symptomProductMatchRepository;
    this.symptomRepository = symptomRepository;
    this.symptomSearchMapper = symptomSearchMapper;
    this.symptomProductMatchMapper = symptomProductMatchMapper;
    this.productCatalogClient = productCatalogClient;
    this.userHealthClient = userHealthClient;
    this.productRanker = productRanker;
  }

  @Transactional(readOnly = true)
  public List<SymptomSearchResponseDto> getSearches(Long userId, Long patientProfileId) {
    List<SymptomSearch> searches =
        userId != null
            ? symptomSearchRepository.findByUserIdOrderBySearchedAtDesc(userId)
            : patientProfileId != null
                ? symptomSearchRepository.findByPatientProfileIdOrderBySearchedAtDesc(
                    patientProfileId)
                : symptomSearchRepository.findAllByOrderBySearchedAtDesc();

    if (userId != null && patientProfileId != null) {
      searches =
          searches.stream()
              .filter(search -> Objects.equals(search.getPatientProfileId(), patientProfileId))
              .toList();
    }

    return searches.stream().map(symptomSearchMapper::toResponseDto).toList();
  }

  @Transactional(readOnly = true)
  public SymptomSearchResponseDto getSearch(Long id) {
    return symptomSearchMapper.toResponseDto(findSearchById(id));
  }

  @Transactional
  public SymptomSearchResponseDto createSearch(SymptomSearchRequestDto requestDto) {
    SymptomSearch search =
        SymptomSearch.builder()
            .userId(requestDto.getUserId())
            .patientProfileId(requestDto.getPatientProfileId())
            .searchQuery(sanitizeRequired(requestDto.getSearchQuery(), "searchQuery"))
            .searchedAt(LocalDateTime.now())
            .build();

    return symptomSearchMapper.toResponseDto(symptomSearchRepository.save(search));
  }

  @Transactional(readOnly = true)
  public List<SymptomSearchItemResponseDto> getItems(Long searchId) {
    findSearchById(searchId);
    return symptomSearchItemRepository.findBySearchSearchIdOrderBySearchItemIdAsc(searchId).stream()
        .map(symptomSearchMapper::toItemResponseDto)
        .toList();
  }

  @Transactional
  public SymptomSearchItemResponseDto addItem(
      Long searchId, SymptomSearchItemRequestDto requestDto) {
    SymptomSearch search = findSearchById(searchId);
    Symptom symptom = findActiveSymptomById(requestDto.getSymptomId());

    if (symptomSearchItemRepository.existsBySearchSearchIdAndSymptomSymptomId(
        searchId, requestDto.getSymptomId())) {
      throw new DuplicateResourceException("Symptom is already linked to this search.");
    }

    SymptomSearchItem item = SymptomSearchItem.builder().search(search).symptom(symptom).build();
    return symptomSearchMapper.toItemResponseDto(symptomSearchItemRepository.save(item));
  }

  @Transactional
  public void deleteItem(Long searchId, Long itemId) {
    SymptomSearchItem item =
        symptomSearchItemRepository
            .findBySearchSearchIdAndSearchItemId(searchId, itemId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Symptom search item not found with id: "
                            + itemId
                            + " for search: "
                            + searchId));
    symptomSearchItemRepository.delete(item);
  }

  @Transactional(readOnly = true)
  public List<SymptomProductMatchResponseDto> getMatches(Long searchId) {
    SymptomSearch search = findSearchById(searchId);
    UserHealthContext userHealthContext =
        loadUserHealthContext(search.getUserId(), search.getPatientProfileId());
    List<Symptom> symptoms =
        symptomSearchItemRepository.findBySearchSearchIdOrderBySearchItemIdAsc(searchId).stream()
            .map(SymptomSearchItem::getSymptom)
            .filter(Symptom::isActive)
            .distinct()
            .toList();
    Map<Long, List<SymptomProductMatch>> curatedMatches =
        symptoms.stream()
            .map(Symptom::getSymptomId)
            .distinct()
            .flatMap(
                symptomId ->
                    symptomProductMatchRepository
                        .findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(symptomId)
                        .stream())
            .collect(
                Collectors.groupingBy(
                    SymptomProductMatch::getProductId, LinkedHashMap::new, Collectors.toList()));

    try {
      List<ProductSnapshot> activeProducts =
          userHealthContext.withoutAllergyConflicts(productCatalogClient.getActiveProducts());
      return productRanker.rankSymptomMatches(symptoms, activeProducts, curatedMatches, 10).stream()
          .map(this::toRankedResponseDto)
          .toList();
    } catch (ExternalServiceException ex) {
      return curatedMatches.values().stream()
          .map(this::toAggregatedResponseDto)
          .sorted(
              Comparator.comparing(
                      SymptomProductMatchResponseDto::getRelevanceScore,
                      Comparator.nullsLast(Comparator.reverseOrder()))
                  .thenComparing(SymptomProductMatchResponseDto::getProductId))
          .toList();
    }
  }

  private SymptomProductMatchResponseDto toRankedResponseDto(ProductScore score) {
    return new SymptomProductMatchResponseDto(
        null,
        score.matchedSymptomIds().stream().findFirst().orElse(null),
        score.product().getId(),
        score.score(),
        score.reason(),
        score.matchedSymptomIds());
  }

  private SymptomProductMatchResponseDto toAggregatedResponseDto(
      List<SymptomProductMatch> productMatches) {
    SymptomProductMatch bestMatch =
        productMatches.stream()
            .sorted(
                Comparator.comparing(
                        SymptomProductMatch::getRelevanceScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SymptomProductMatch::getMatchId))
            .findFirst()
            .orElseThrow(
                () -> new BadRequestException("Unable to aggregate empty product matches."));

    SymptomProductMatchResponseDto responseDto = symptomProductMatchMapper.toResponseDto(bestMatch);
    double combinedScore =
        productMatches.stream()
            .map(SymptomProductMatch::getRelevanceScore)
            .filter(Objects::nonNull)
            .reduce(0.0, Double::sum);
    responseDto.setRelevanceScore(Math.min(combinedScore, 1.0));
    responseDto.setMatchReason(
        productMatches.stream()
            .map(SymptomProductMatch::getMatchReason)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(reason -> !reason.isEmpty())
            .distinct()
            .collect(Collectors.joining(" | ")));
    responseDto.setMatchedSymptomIds(
        productMatches.stream()
            .map(match -> match.getSymptom().getSymptomId())
            .distinct()
            .sorted()
            .toList());
    return responseDto;
  }

  private SymptomSearch findSearchById(Long id) {
    return symptomSearchRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Symptom search not found with id: " + id));
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

  private UserHealthContext loadUserHealthContext(Long userId, Long patientProfileId) {
    UserHealthSnapshot user =
        userHealthClient
            .getUser(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    if (patientProfileId == null) {
      return UserHealthContext.from(user.getPatientProfile());
    }

    PatientHealthProfileSnapshot profile =
        userHealthClient
            .getPatientProfile(patientProfileId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Patient profile not found with id: " + patientProfileId));
    return UserHealthContext.from(profile);
  }

  private String sanitizeRequired(String value, String fieldName) {
    String sanitized = TextSanitizer.sanitizeRequiredText(value);
    if (sanitized == null) {
      throw new BadRequestException(fieldName + " is required.");
    }
    return sanitized;
  }
}

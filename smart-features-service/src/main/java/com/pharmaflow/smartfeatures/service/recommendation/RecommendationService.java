package com.pharmaflow.smartfeatures.service.recommendation;

import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationEventResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationGenerateRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationInteractionRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.recommendation.RecommendationMapper;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.model.recommendation.RecommendationEvent;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearch;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationEventRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomProductMatchRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchItemRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchRepository;
import com.pharmaflow.smartfeatures.service.productintelligence.ProductRanker;
import com.pharmaflow.smartfeatures.service.productintelligence.ProductScore;
import com.pharmaflow.smartfeatures.service.productintelligence.UserHealthContext;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

  private final RecommendationRepository recommendationRepository;
  private final RecommendationEventRepository recommendationEventRepository;
  private final RecommendationMapper recommendationMapper;
  private final ProductCatalogClient productCatalogClient;
  private final UserHealthClient userHealthClient;
  private final ProductRanker productRanker;
  private final SymptomRepository symptomRepository;
  private final SymptomProductMatchRepository symptomProductMatchRepository;
  private final SymptomSearchRepository symptomSearchRepository;
  private final SymptomSearchItemRepository symptomSearchItemRepository;

  public RecommendationService(
      RecommendationRepository recommendationRepository,
      RecommendationEventRepository recommendationEventRepository,
      RecommendationMapper recommendationMapper,
      ProductCatalogClient productCatalogClient,
      UserHealthClient userHealthClient,
      ProductRanker productRanker,
      SymptomRepository symptomRepository,
      SymptomProductMatchRepository symptomProductMatchRepository,
      SymptomSearchRepository symptomSearchRepository,
      SymptomSearchItemRepository symptomSearchItemRepository) {
    this.recommendationRepository = recommendationRepository;
    this.recommendationEventRepository = recommendationEventRepository;
    this.recommendationMapper = recommendationMapper;
    this.productCatalogClient = productCatalogClient;
    this.userHealthClient = userHealthClient;
    this.productRanker = productRanker;
    this.symptomRepository = symptomRepository;
    this.symptomProductMatchRepository = symptomProductMatchRepository;
    this.symptomSearchRepository = symptomSearchRepository;
    this.symptomSearchItemRepository = symptomSearchItemRepository;
  }

  @Transactional
  public List<RecommendationResponseDto> getRecommendations(Long userId, Long patientProfileId) {
    List<Recommendation> recommendations =
        userId != null
            ? recommendationRepository.findByUserIdOrderByGeneratedAtDesc(userId)
            : patientProfileId != null
                ? recommendationRepository.findByPatientProfileIdOrderByGeneratedAtDesc(
                    patientProfileId)
                : recommendationRepository.findAllByOrderByGeneratedAtDesc();

    if (userId != null && patientProfileId != null) {
      recommendations =
          recommendations.stream()
              .filter(
                  recommendation -> patientProfileId.equals(recommendation.getPatientProfileId()))
              .toList();
    }

    recommendations.forEach(this::refreshStatusIfExpired);
    return recommendations.stream().map(recommendationMapper::toResponseDto).toList();
  }

  @Transactional
  public RecommendationResponseDto getRecommendation(Long id) {
    Recommendation recommendation = findRecommendationById(id);
    refreshStatusIfExpired(recommendation);
    return recommendationMapper.toResponseDto(recommendation);
  }

  @Transactional
  public RecommendationResponseDto createRecommendation(RecommendationRequestDto requestDto) {
    ensureActiveRecommendationIsUnique(requestDto, null);

    Recommendation recommendation = recommendationMapper.toEntity(requestDto);
    recommendation.setReasonText(TextSanitizer.sanitizeOptionalText(requestDto.getReasonText()));
    recommendation.setGeneratedAt(LocalDateTime.now());
    validateExpiresAt(recommendation.getGeneratedAt(), recommendation.getExpiresAt());
    recommendation.setStatus(RecommendationStatus.ACTIVE);

    return recommendationMapper.toResponseDto(recommendationRepository.save(recommendation));
  }

  @Transactional
  public List<RecommendationResponseDto> generateRecommendations(
      RecommendationGenerateRequestDto requestDto) {
    validateGenerateRequest(requestDto);
    int limit = requestDto.getLimit() == null ? 10 : requestDto.getLimit();
    Set<Long> positiveProductIds = positiveProductIds(requestDto.getUserId());
    Set<Long> dismissedProductIds = dismissedProductIds(requestDto.getUserId());
    UserHealthContext userHealthContext =
        loadUserHealthContext(requestDto.getUserId(), requestDto.getPatientProfileId());
    List<ProductSnapshot> activeProducts =
        userHealthContext.withoutAllergyConflicts(productCatalogClient.getActiveProducts());

    List<ProductScore> scores =
        switch (requestDto.getRecommendationType()) {
          case SIMILAR_PRODUCT -> {
            ProductSnapshot seed = loadSeedProduct(requestDto.getSeedProductId());
            yield productRanker.rankSimilarProducts(
                seed, activeProducts, positiveProductIds, dismissedProductIds, limit);
          }
          case ALTERNATIVE -> {
            ProductSnapshot seed = loadSeedProduct(requestDto.getSeedProductId());
            yield productRanker.rankAlternativeProducts(
                seed,
                activeProducts,
                productCatalogClient.getSubstitutesForProduct(seed.getId()),
                positiveProductIds,
                dismissedProductIds,
                limit);
          }
          case FOR_YOU -> {
            List<Symptom> symptoms = loadSymptomsForGeneration(requestDto);
            yield productRanker.rankPersonalizedProducts(
                symptoms,
                activeProducts,
                curatedMatchesByProduct(symptoms),
                positiveProductIds,
                dismissedProductIds,
                limit);
          }
          case FREQUENTLY_BOUGHT_TOGETHER, SEASONAL ->
              throw new BadRequestException(
                  "Generation is not supported for recommendationType: "
                      + requestDto.getRecommendationType());
        };

    return scores.stream()
        .map(score -> upsertGeneratedRecommendation(requestDto, score))
        .map(recommendationMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public RecommendationResponseDto updateRecommendation(
      Long id, RecommendationRequestDto requestDto) {
    Recommendation recommendation = findRecommendationById(id);
    refreshStatusIfExpired(recommendation);
    if (recommendation.getStatus() != RecommendationStatus.ACTIVE) {
      throw new BadRequestException("Only active recommendations can be updated.");
    }

    ensureActiveRecommendationIsUnique(requestDto, id);
    recommendationMapper.updateEntity(requestDto, recommendation);
    recommendation.setReasonText(TextSanitizer.sanitizeOptionalText(requestDto.getReasonText()));
    validateExpiresAt(recommendation.getGeneratedAt(), recommendation.getExpiresAt());

    return recommendationMapper.toResponseDto(recommendationRepository.save(recommendation));
  }

  @Transactional(noRollbackFor = BadRequestException.class)
  public RecommendationEventResponseDto logInteraction(
      Long recommendationId, RecommendationInteractionRequestDto requestDto) {
    Recommendation recommendation = findRecommendationById(recommendationId);
    refreshStatusIfExpired(recommendation);
    if (recommendation.getStatus() != RecommendationStatus.ACTIVE) {
      throw new BadRequestException("Only active recommendations can receive interactions.");
    }

    RecommendationEvent event =
        RecommendationEvent.builder()
            .recommendation(recommendation)
            .eventType(requestDto.getInteractionType())
            .eventTime(LocalDateTime.now())
            .build();

    RecommendationEvent savedEvent = recommendationEventRepository.save(event);
    if (requestDto.getInteractionType() == RecommendationEventType.DISMISSED) {
      recommendation.setStatus(RecommendationStatus.DISMISSED);
      recommendationRepository.save(recommendation);
    }

    return recommendationMapper.toEventResponseDto(savedEvent);
  }

  @Transactional(readOnly = true)
  public List<RecommendationEventResponseDto> getInteractions(Long recommendationId) {
    findRecommendationById(recommendationId);
    return recommendationEventRepository
        .findByRecommendationRecommendationIdOrderByEventTimeDesc(recommendationId)
        .stream()
        .map(recommendationMapper::toEventResponseDto)
        .toList();
  }

  private Recommendation findRecommendationById(Long id) {
    return recommendationRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Recommendation not found with id: " + id));
  }

  private void validateGenerateRequest(RecommendationGenerateRequestDto requestDto) {
    if ((requestDto.getRecommendationType() == RecommendationType.SIMILAR_PRODUCT
            || requestDto.getRecommendationType() == RecommendationType.ALTERNATIVE)
        && requestDto.getSeedProductId() == null) {
      throw new BadRequestException(
          "seedProductId is required for " + requestDto.getRecommendationType() + ".");
    }
    if (requestDto.getExpiresAt() != null
        && requestDto.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BadRequestException("expiresAt must not be in the past.");
    }
  }

  private ProductSnapshot loadSeedProduct(Long productId) {
    return productCatalogClient
        .getProduct(productId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Product not found with id: " + productId));
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

  private Recommendation upsertGeneratedRecommendation(
      RecommendationGenerateRequestDto requestDto, ProductScore score) {
    Recommendation recommendation =
        findActiveRecommendationForDimensions(
                requestDto.getUserId(),
                requestDto.getPatientProfileId(),
                score.product().getId(),
                requestDto.getRecommendationType())
            .orElseGet(Recommendation::new);

    recommendation.setUserId(requestDto.getUserId());
    recommendation.setPatientProfileId(requestDto.getPatientProfileId());
    recommendation.setProductId(score.product().getId());
    recommendation.setRecommendationType(requestDto.getRecommendationType());
    recommendation.setScore(score.score());
    recommendation.setReasonText(TextSanitizer.sanitizeOptionalText(score.reason()));
    recommendation.setGeneratedAt(LocalDateTime.now());
    recommendation.setExpiresAt(requestDto.getExpiresAt());
    recommendation.setStatus(RecommendationStatus.ACTIVE);
    return recommendationRepository.save(recommendation);
  }

  private java.util.Optional<Recommendation> findActiveRecommendationForDimensions(
      Long userId, Long patientProfileId, Long productId, RecommendationType recommendationType) {
    return patientProfileId == null
        ? recommendationRepository.findByUserIdAndProductIdAndRecommendationTypeAndStatus(
            userId, productId, recommendationType, RecommendationStatus.ACTIVE)
        : recommendationRepository
            .findByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatus(
                userId,
                patientProfileId,
                productId,
                recommendationType,
                RecommendationStatus.ACTIVE);
  }

  private List<Symptom> loadSymptomsForGeneration(RecommendationGenerateRequestDto requestDto) {
    if (requestDto.getSymptomIds() != null && !requestDto.getSymptomIds().isEmpty()) {
      return requestDto.getSymptomIds().stream()
          .distinct()
          .map(this::findActiveSymptomById)
          .toList();
    }

    List<SymptomSearch> searches =
        requestDto.getPatientProfileId() != null
            ? symptomSearchRepository.findByPatientProfileIdOrderBySearchedAtDesc(
                requestDto.getPatientProfileId())
            : symptomSearchRepository.findByUserIdOrderBySearchedAtDesc(requestDto.getUserId());
    return searches.stream()
        .limit(5)
        .flatMap(
            search ->
                symptomSearchItemRepository
                    .findBySearchSearchIdOrderBySearchItemIdAsc(search.getSearchId())
                    .stream())
        .map(item -> item.getSymptom())
        .filter(Objects::nonNull)
        .filter(Symptom::isActive)
        .collect(
            Collectors.toMap(
                Symptom::getSymptomId,
                symptom -> symptom,
                (existing, replacement) -> existing,
                LinkedHashMap::new))
        .values()
        .stream()
        .toList();
  }

  private Symptom findActiveSymptomById(Long symptomId) {
    Symptom symptom =
        symptomRepository
            .findById(symptomId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Symptom not found with id: " + symptomId));
    if (!symptom.isActive()) {
      throw new ResourceNotFoundException("Symptom not found with id: " + symptomId);
    }
    return symptom;
  }

  private Map<Long, List<SymptomProductMatch>> curatedMatchesByProduct(List<Symptom> symptoms) {
    return symptoms.stream()
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
  }

  private Set<Long> positiveProductIds(Long userId) {
    return recommendationEventRepository
        .findByRecommendationUserIdOrderByEventTimeDesc(userId)
        .stream()
        .filter(
            event ->
                event.getEventType() == RecommendationEventType.CLICKED
                    || event.getEventType() == RecommendationEventType.ADDED_TO_CART
                    || event.getEventType() == RecommendationEventType.PURCHASED)
        .map(event -> event.getRecommendation().getProductId())
        .collect(Collectors.toSet());
  }

  private Set<Long> dismissedProductIds(Long userId) {
    Set<Long> dismissedByStatus =
        recommendationRepository.findByUserIdOrderByGeneratedAtDesc(userId).stream()
            .filter(recommendation -> recommendation.getStatus() == RecommendationStatus.DISMISSED)
            .map(Recommendation::getProductId)
            .collect(Collectors.toSet());
    Set<Long> dismissedByEvent =
        recommendationEventRepository
            .findByRecommendationUserIdOrderByEventTimeDesc(userId)
            .stream()
            .filter(event -> event.getEventType() == RecommendationEventType.DISMISSED)
            .map(event -> event.getRecommendation().getProductId())
            .collect(Collectors.toSet());
    dismissedByStatus.addAll(dismissedByEvent);
    return dismissedByStatus;
  }

  private void validateExpiresAt(LocalDateTime generatedAt, LocalDateTime expiresAt) {
    if (expiresAt != null && expiresAt.isBefore(generatedAt)) {
      throw new BadRequestException("expiresAt must not be before generatedAt.");
    }
  }

  private void refreshStatusIfExpired(Recommendation recommendation) {
    if (recommendation.getStatus() == RecommendationStatus.ACTIVE
        && recommendation.getExpiresAt() != null
        && recommendation.getExpiresAt().isBefore(LocalDateTime.now())) {
      recommendation.setStatus(RecommendationStatus.EXPIRED);
      recommendationRepository.save(recommendation);
    }
  }

  private void ensureActiveRecommendationIsUnique(
      RecommendationRequestDto requestDto, Long currentId) {
    boolean duplicateExists;
    if (requestDto.getPatientProfileId() == null) {
      duplicateExists =
          currentId == null
              ? recommendationRepository.existsByUserIdAndProductIdAndRecommendationTypeAndStatus(
                  requestDto.getUserId(),
                  requestDto.getProductId(),
                  requestDto.getRecommendationType(),
                  RecommendationStatus.ACTIVE)
              : recommendationRepository
                  .existsByUserIdAndProductIdAndRecommendationTypeAndStatusAndRecommendationIdNot(
                      requestDto.getUserId(),
                      requestDto.getProductId(),
                      requestDto.getRecommendationType(),
                      RecommendationStatus.ACTIVE,
                      currentId);
    } else {
      duplicateExists =
          currentId == null
              ? recommendationRepository
                  .existsByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatus(
                      requestDto.getUserId(),
                      requestDto.getPatientProfileId(),
                      requestDto.getProductId(),
                      requestDto.getRecommendationType(),
                      RecommendationStatus.ACTIVE)
              : recommendationRepository
                  .existsByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatusAndRecommendationIdNot(
                      requestDto.getUserId(),
                      requestDto.getPatientProfileId(),
                      requestDto.getProductId(),
                      requestDto.getRecommendationType(),
                      RecommendationStatus.ACTIVE,
                      currentId);
    }

    if (duplicateExists) {
      throw new DuplicateResourceException(
          "An active recommendation with the same dimensions already exists.");
    }
  }
}

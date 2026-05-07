package com.pharmaflow.smartfeatures.service.fraud;

import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudLogResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleResponseDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.enums.fraud.FraudRuleCode;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.fraud.FraudMapper;
import com.pharmaflow.smartfeatures.model.fraud.FraudCheck;
import com.pharmaflow.smartfeatures.model.fraud.FraudLog;
import com.pharmaflow.smartfeatures.model.fraud.FraudRule;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudCheckRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudLogRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudRuleRepository;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudService {

  private final FraudRuleRepository fraudRuleRepository;
  private final FraudCheckRepository fraudCheckRepository;
  private final FraudLogRepository fraudLogRepository;
  private final FraudMapper fraudMapper;
  private final FraudRuleEvaluator fraudRuleEvaluator;

  public FraudService(
      FraudRuleRepository fraudRuleRepository,
      FraudCheckRepository fraudCheckRepository,
      FraudLogRepository fraudLogRepository,
      FraudMapper fraudMapper,
      FraudRuleEvaluator fraudRuleEvaluator) {
    this.fraudRuleRepository = fraudRuleRepository;
    this.fraudCheckRepository = fraudCheckRepository;
    this.fraudLogRepository = fraudLogRepository;
    this.fraudMapper = fraudMapper;
    this.fraudRuleEvaluator = fraudRuleEvaluator;
  }

  @Transactional(readOnly = true)
  public List<FraudRuleResponseDto> getRules() {
    return fraudRuleRepository.findAllByOrderByRuleNameAsc().stream()
        .map(fraudMapper::toResponseDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public FraudRuleResponseDto getRule(Long id) {
    return fraudMapper.toResponseDto(findRuleById(id));
  }

  @Transactional
  public FraudRuleResponseDto createRule(FraudRuleRequestDto requestDto) {
    ensureRuleNameUnique(TextSanitizer.normalizeKey(requestDto.getRuleName()), null);
    String ruleCode = sanitizeRuleCode(requestDto.getRuleCode());
    ensureSupportedRuleCode(ruleCode);
    ensureRuleCodeUnique(ruleCode, null);
    FraudRule rule = fraudMapper.toEntity(requestDto);
    rule.setRuleName(sanitizeRequired(requestDto.getRuleName(), "ruleName"));
    rule.setRuleCode(ruleCode);
    rule.setCategory(sanitizeRuleCategory(requestDto.getCategory(), ruleCode));
    rule.setDescription(TextSanitizer.sanitizeOptionalText(requestDto.getDescription()));
    return fraudMapper.toResponseDto(fraudRuleRepository.save(rule));
  }

  @Transactional
  public FraudRuleResponseDto updateRule(Long id, FraudRuleRequestDto requestDto) {
    FraudRule rule = findRuleById(id);
    ensureRuleNameUnique(TextSanitizer.normalizeKey(requestDto.getRuleName()), id);
    String ruleCode = sanitizeRuleCode(requestDto.getRuleCode());
    ensureSupportedRuleCode(ruleCode);
    ensureRuleCodeUnique(ruleCode, id);
    fraudMapper.updateEntity(requestDto, rule);
    rule.setRuleName(sanitizeRequired(requestDto.getRuleName(), "ruleName"));
    rule.setRuleCode(ruleCode);
    rule.setCategory(sanitizeRuleCategory(requestDto.getCategory(), ruleCode));
    rule.setDescription(TextSanitizer.sanitizeOptionalText(requestDto.getDescription()));
    return fraudMapper.toResponseDto(fraudRuleRepository.save(rule));
  }

  @Transactional
  public void deleteRule(Long id) {
    fraudRuleRepository.delete(findRuleById(id));
  }

  @Transactional
  public FraudCheckResponseDto createCheck(FraudCheckRequestDto requestDto) {
    ensureDefaultRulesExist();
    List<FraudRule> activeRules = fraudRuleRepository.findByIsActiveTrueOrderByRuleNameAsc();
    FraudRuleEvaluator.FraudEvaluation evaluation =
        fraudRuleEvaluator.evaluate(requestDto.getOrderId(), activeRules);

    FraudCheck fraudCheck =
        FraudCheck.builder()
            .userId(evaluation.userId())
            .orderId(requestDto.getOrderId())
            .checkedAt(LocalDateTime.now())
            .riskScore(Math.min(evaluation.riskScore(), 100.0))
            .decision(resolveDecision(evaluation.riskScore()))
            .build();
    FraudCheck savedCheck = fraudCheckRepository.save(fraudCheck);

    LocalDateTime logTime = LocalDateTime.now();
    List<FraudLog> logs =
        evaluation.outcomes().stream()
            .map(
                outcome ->
                    FraudLog.builder()
                        .fraudCheck(savedCheck)
                        .fraudRule(outcome.rule())
                        .eventType(outcome.eventType())
                        .details(outcome.details())
                        .scoreContribution(outcome.scoreContribution())
                        .createdAt(logTime)
                        .build())
            .toList();

    fraudLogRepository.saveAll(logs);
    return fraudMapper.toResponseDto(savedCheck);
  }

  @Transactional(readOnly = true)
  public FraudCheckResponseDto getCheck(Long id) {
    return fraudMapper.toResponseDto(findCheckById(id));
  }

  @Transactional(readOnly = true)
  public List<FraudCheckResponseDto> getChecks(Long userId, Long orderId) {
    List<FraudCheck> checks =
        userId != null
            ? fraudCheckRepository.findByUserIdOrderByCheckedAtDesc(userId)
            : orderId != null
                ? fraudCheckRepository.findByOrderIdOrderByCheckedAtDesc(orderId)
                : fraudCheckRepository.findAll();
    return checks.stream()
        .sorted(Comparator.comparing(FraudCheck::getCheckedAt).reversed())
        .map(fraudMapper::toResponseDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FraudLogResponseDto> getLogsByCheck(Long checkId) {
    findCheckById(checkId);
    return fraudLogRepository.findByFraudCheckFraudCheckIdOrderByCreatedAtDesc(checkId).stream()
        .map(fraudMapper::toResponseDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FraudLogResponseDto> getLogsByRule(Long ruleId) {
    findRuleById(ruleId);
    return fraudLogRepository.findByFraudRuleRuleIdOrderByCreatedAtDesc(ruleId).stream()
        .map(fraudMapper::toResponseDto)
        .toList();
  }

  private FraudRule findRuleById(Long id) {
    return fraudRuleRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Fraud rule not found with id: " + id));
  }

  private FraudCheck findCheckById(Long id) {
    return fraudCheckRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Fraud check not found with id: " + id));
  }

  private void ensureRuleNameUnique(String normalizedRuleName, Long currentId) {
    boolean duplicateExists =
        currentId == null
            ? fraudRuleRepository.existsByNormalizedRuleName(normalizedRuleName)
            : fraudRuleRepository.existsByNormalizedRuleNameAndRuleIdNot(
                normalizedRuleName, currentId);

    if (duplicateExists) {
      throw new DuplicateResourceException("Fraud rule with the same name already exists.");
    }
  }

  private void ensureRuleCodeUnique(String ruleCode, Long currentId) {
    boolean duplicateExists =
        currentId == null
            ? fraudRuleRepository.existsByRuleCode(ruleCode)
            : fraudRuleRepository.existsByRuleCodeAndRuleIdNot(ruleCode, currentId);

    if (duplicateExists) {
      throw new DuplicateResourceException("Fraud rule with the same code already exists.");
    }
  }

  private void ensureDefaultRulesExist() {
    for (FraudRuleCode ruleCode : FraudRuleCode.values()) {
      if (!fraudRuleRepository.existsByRuleCode(ruleCode.name())) {
        fraudRuleRepository.save(
            FraudRule.builder()
                .ruleName(ruleCode.getDefaultRuleName())
                .ruleCode(ruleCode.name())
                .category(ruleCode.getCategory())
                .description("Default fraud rule: " + ruleCode.getDefaultRuleName())
                .weight(ruleCode.getDefaultWeight())
                .isActive(true)
                .build());
      }
    }
  }

  private void ensureSupportedRuleCode(String ruleCode) {
    if (FraudRuleCode.fromCode(ruleCode).isEmpty()) {
      throw new BadRequestException("ruleCode is not supported by the fraud evaluator.");
    }
  }

  private FraudDecision resolveDecision(double riskScore) {
    if (riskScore >= 70.0) {
      return FraudDecision.BLOCKED;
    }
    if (riskScore >= 30.0) {
      return FraudDecision.REVIEW;
    }
    return FraudDecision.APPROVED;
  }

  private String sanitizeRequired(String value, String fieldName) {
    String sanitized = TextSanitizer.sanitizeRequiredText(value);
    if (sanitized == null) {
      throw new BadRequestException(fieldName + " is required.");
    }
    return sanitized;
  }

  private String sanitizeRuleCode(String value) {
    String sanitized = sanitizeRequired(value, "ruleCode");
    return sanitized.replace('-', '_').replace(' ', '_').toUpperCase();
  }

  private String sanitizeRuleCategory(String category, String ruleCode) {
    String sanitized = sanitizeRequired(category, "category");
    FraudRuleCode.fromCode(ruleCode)
        .ifPresent(
            code -> {
              if (!code.getCategory().equalsIgnoreCase(sanitized)) {
                throw new BadRequestException(
                    "category must match the supported ruleCode category.");
              }
            });
    return sanitized.replace('-', '_').replace(' ', '_').toUpperCase();
  }
}

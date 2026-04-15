package com.pharmaflow.smartfeatures.service.fraud;

import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudLogResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleResponseDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
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
import java.util.ArrayList;
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

    public FraudService(
            FraudRuleRepository fraudRuleRepository,
            FraudCheckRepository fraudCheckRepository,
            FraudLogRepository fraudLogRepository,
            FraudMapper fraudMapper) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudCheckRepository = fraudCheckRepository;
        this.fraudLogRepository = fraudLogRepository;
        this.fraudMapper = fraudMapper;
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
        FraudRule rule = fraudMapper.toEntity(requestDto);
        rule.setRuleName(sanitizeRequired(requestDto.getRuleName(), "ruleName"));
        rule.setDescription(TextSanitizer.sanitizeOptionalText(requestDto.getDescription()));
        return fraudMapper.toResponseDto(fraudRuleRepository.save(rule));
    }

    @Transactional
    public FraudRuleResponseDto updateRule(Long id, FraudRuleRequestDto requestDto) {
        FraudRule rule = findRuleById(id);
        ensureRuleNameUnique(TextSanitizer.normalizeKey(requestDto.getRuleName()), id);
        fraudMapper.updateEntity(requestDto, rule);
        rule.setRuleName(sanitizeRequired(requestDto.getRuleName(), "ruleName"));
        rule.setDescription(TextSanitizer.sanitizeOptionalText(requestDto.getDescription()));
        return fraudMapper.toResponseDto(fraudRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        fraudRuleRepository.delete(findRuleById(id));
    }

    @Transactional
    public FraudCheckResponseDto createCheck(FraudCheckRequestDto requestDto) {
        FraudCheck fraudCheck = FraudCheck.builder()
                .userId(requestDto.getUserId())
                .orderId(requestDto.getOrderId())
                .checkedAt(LocalDateTime.now())
                .riskScore(0.0)
                .decision(FraudDecision.APPROVED)
                .build();
        FraudCheck savedCheck = fraudCheckRepository.save(fraudCheck);

        double riskScore = 0.0;
        List<FraudLog> logs = new ArrayList<>();
        for (FraudRule rule : fraudRuleRepository.findByIsActiveTrueOrderByRuleNameAsc()) {
            boolean triggered = ((requestDto.getUserId() + requestDto.getOrderId() + rule.getRuleId()) % 2) == 0;
            if (triggered) {
                riskScore += rule.getWeight();
            }
            logs.add(FraudLog.builder()
                    .fraudCheck(savedCheck)
                    .fraudRule(rule)
                    .eventType(triggered ? FraudEventType.TRIGGERED : FraudEventType.CLEARED)
                    .details(triggered
                            ? "Rule triggered during deterministic local evaluation."
                            : "Rule did not trigger during deterministic local evaluation.")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        fraudLogRepository.saveAll(logs);
        savedCheck.setRiskScore(Math.min(riskScore, 100.0));
        savedCheck.setDecision(resolveDecision(savedCheck.getRiskScore()));
        return fraudMapper.toResponseDto(fraudCheckRepository.save(savedCheck));
    }

    @Transactional(readOnly = true)
    public FraudCheckResponseDto getCheck(Long id) {
        return fraudMapper.toResponseDto(findCheckById(id));
    }

    @Transactional(readOnly = true)
    public List<FraudCheckResponseDto> getChecks(Long userId, Long orderId) {
        List<FraudCheck> checks = userId != null
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
        return fraudRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud rule not found with id: " + id));
    }

    private FraudCheck findCheckById(Long id) {
        return fraudCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud check not found with id: " + id));
    }

    private void ensureRuleNameUnique(String normalizedRuleName, Long currentId) {
        boolean duplicateExists = currentId == null
                ? fraudRuleRepository.existsByNormalizedRuleName(normalizedRuleName)
                : fraudRuleRepository.existsByNormalizedRuleNameAndRuleIdNot(normalizedRuleName, currentId);

        if (duplicateExists) {
            throw new DuplicateResourceException("Fraud rule with the same name already exists.");
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
}

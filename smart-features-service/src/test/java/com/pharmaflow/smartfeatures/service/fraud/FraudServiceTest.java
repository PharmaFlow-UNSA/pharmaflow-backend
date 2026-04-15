package com.pharmaflow.smartfeatures.service.fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleRequestDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.mapper.fraud.FraudMapper;
import com.pharmaflow.smartfeatures.model.fraud.FraudCheck;
import com.pharmaflow.smartfeatures.model.fraud.FraudLog;
import com.pharmaflow.smartfeatures.model.fraud.FraudRule;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudCheckRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudLogRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudRuleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private FraudCheckRepository fraudCheckRepository;

    @Mock
    private FraudLogRepository fraudLogRepository;

    private FraudService fraudService;

    @BeforeEach
    void setUp() {
        fraudService = new FraudService(
                fraudRuleRepository,
                fraudCheckRepository,
                fraudLogRepository,
                new FraudMapper(new ModelMapperConfig().modelMapper()));
    }

    @Test
    void createCheckShouldCalculateRiskAndPersistLogs() {
        FraudRule triggeredRule = FraudRule.builder()
                .ruleId(2L)
                .ruleName("High value")
                .weight(80.0)
                .isActive(true)
                .build();
        FraudRule clearedRule = FraudRule.builder()
                .ruleId(1L)
                .ruleName("Late pickup")
                .weight(15.0)
                .isActive(true)
                .build();

        when(fraudRuleRepository.findByIsActiveTrueOrderByRuleNameAsc()).thenReturn(List.of(triggeredRule, clearedRule));
        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> {
            FraudCheck fraudCheck = invocation.getArgument(0);
            if (fraudCheck.getFraudCheckId() == null) {
                fraudCheck.setFraudCheckId(11L);
            }
            return fraudCheck;
        });
        when(fraudLogRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FraudCheckResponseDto response = fraudService.createCheck(new FraudCheckRequestDto(1L, 1L));

        ArgumentCaptor<List<FraudLog>> logCaptor = ArgumentCaptor.forClass(List.class);
        verify(fraudLogRepository).saveAll(logCaptor.capture());
        assertThat(logCaptor.getValue()).hasSize(2);
        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getRiskScore()).isEqualTo(80.0);
        assertThat(response.getDecision()).isEqualTo(FraudDecision.BLOCKED);
    }

    @Test
    void createRuleShouldRejectDuplicateNormalizedName() {
        FraudRuleRequestDto requestDto = new FraudRuleRequestDto(" Late pickup ", "Delayed pickup", 35.0, true);
        when(fraudRuleRepository.existsByNormalizedRuleName("late pickup")).thenReturn(true);

        assertThatThrownBy(() -> fraudService.createRule(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Fraud rule with the same name already exists.");

        verify(fraudRuleRepository, never()).save(any(FraudRule.class));
    }
}

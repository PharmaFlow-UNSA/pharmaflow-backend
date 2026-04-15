package com.pharmaflow.smartfeatures.mapper.fraud;

import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudLogResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleResponseDto;
import com.pharmaflow.smartfeatures.model.fraud.FraudCheck;
import com.pharmaflow.smartfeatures.model.fraud.FraudLog;
import com.pharmaflow.smartfeatures.model.fraud.FraudRule;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class FraudMapper {

    private final ModelMapper modelMapper;

    public FraudMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public FraudRule toEntity(FraudRuleRequestDto requestDto) {
        FraudRule entity = modelMapper.map(requestDto, FraudRule.class);
        entity.setActive(Boolean.TRUE.equals(requestDto.getActive()));
        return entity;
    }

    public void updateEntity(FraudRuleRequestDto requestDto, FraudRule rule) {
        modelMapper.map(requestDto, rule);
        rule.setActive(Boolean.TRUE.equals(requestDto.getActive()));
    }

    public FraudRuleResponseDto toResponseDto(FraudRule rule) {
        FraudRuleResponseDto response = modelMapper.map(rule, FraudRuleResponseDto.class);
        response.setId(rule.getRuleId());
        response.setActive(rule.isActive());
        return response;
    }

    public FraudCheckResponseDto toResponseDto(FraudCheck fraudCheck) {
        FraudCheckResponseDto response = modelMapper.map(fraudCheck, FraudCheckResponseDto.class);
        response.setId(fraudCheck.getFraudCheckId());
        return response;
    }

    public FraudLogResponseDto toResponseDto(FraudLog fraudLog) {
        FraudLogResponseDto response = modelMapper.map(fraudLog, FraudLogResponseDto.class);
        response.setId(fraudLog.getFraudLogId());
        response.setFraudCheckId(fraudLog.getFraudCheck().getFraudCheckId());
        response.setFraudRuleId(fraudLog.getFraudRule().getRuleId());
        return response;
    }
}

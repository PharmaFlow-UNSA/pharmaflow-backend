package com.pharmaflow.smartfeatures.dto.fraud;

import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FraudLogResponseDto {

    private Long id;
    private Long fraudCheckId;
    private Long fraudRuleId;
    private FraudEventType eventType;
    private String details;
    private LocalDateTime createdAt;
}

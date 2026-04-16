package com.pharmaflow.smartfeatures.controller.fraud;

import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudLogResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleRequestDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleResponseDto;
import com.pharmaflow.smartfeatures.service.fraud.FraudService;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Fraud")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping("/api/fraud-rules")
    public ResponseEntity<List<FraudRuleResponseDto>> getRules() {
        return ResponseEntity.ok(fraudService.getRules());
    }

    @GetMapping("/api/fraud-rules/{id}")
    public ResponseEntity<FraudRuleResponseDto> getRule(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(fraudService.getRule(id));
    }

    @PostMapping("/api/fraud-rules")
    public ResponseEntity<FraudRuleResponseDto> createRule(@Valid @RequestBody FraudRuleRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fraudService.createRule(requestDto));
    }

    @PutMapping("/api/fraud-rules/{id}")
    public ResponseEntity<FraudRuleResponseDto> updateRule(
            @PathVariable @Positive Long id, @Valid @RequestBody FraudRuleRequestDto requestDto) {
        return ResponseEntity.ok(fraudService.updateRule(id, requestDto));
    }

    @DeleteMapping("/api/fraud-rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable @Positive Long id) {
        fraudService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/fraud-rules/{id}/logs")
    public ResponseEntity<List<FraudLogResponseDto>> getRuleLogs(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(fraudService.getLogsByRule(id));
    }

    @PostMapping("/api/fraud-checks")
    public ResponseEntity<FraudCheckResponseDto> createCheck(@Valid @RequestBody FraudCheckRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fraudService.createCheck(requestDto));
    }

    @GetMapping("/api/fraud-checks/{id}")
    public ResponseEntity<FraudCheckResponseDto> getCheck(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(fraudService.getCheck(id));
    }

    @GetMapping("/api/fraud-checks")
    public ResponseEntity<List<FraudCheckResponseDto>> getChecks(
            @RequestParam(required = false) @NullablePositive Long userId,
            @RequestParam(required = false) @NullablePositive Long orderId) {
        return ResponseEntity.ok(fraudService.getChecks(userId, orderId));
    }

    @GetMapping("/api/fraud-checks/{id}/logs")
    public ResponseEntity<List<FraudLogResponseDto>> getCheckLogs(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(fraudService.getLogsByCheck(id));
    }
}

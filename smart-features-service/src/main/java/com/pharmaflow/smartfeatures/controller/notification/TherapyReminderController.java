package com.pharmaflow.smartfeatures.controller.notification;

import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.TherapyReminderResponseDto;
import com.pharmaflow.smartfeatures.service.notification.TherapyReminderService;
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
@RequestMapping("/api/reminders")
@Tag(name = "Reminders")
public class TherapyReminderController {

    private final TherapyReminderService therapyReminderService;

    public TherapyReminderController(TherapyReminderService therapyReminderService) {
        this.therapyReminderService = therapyReminderService;
    }

    @GetMapping
    public ResponseEntity<List<TherapyReminderResponseDto>> getReminders(
            @RequestParam(required = false) @NullablePositive Long patientProfileId) {
        return ResponseEntity.ok(therapyReminderService.getReminders(patientProfileId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TherapyReminderResponseDto> getReminder(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(therapyReminderService.getReminder(id));
    }

    @PostMapping
    public ResponseEntity<TherapyReminderResponseDto> createReminder(
            @Valid @RequestBody TherapyReminderRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(therapyReminderService.createReminder(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TherapyReminderResponseDto> updateReminder(
            @PathVariable @Positive Long id, @Valid @RequestBody TherapyReminderRequestDto requestDto) {
        return ResponseEntity.ok(therapyReminderService.updateReminder(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable @Positive Long id) {
        therapyReminderService.deleteReminder(id);
        return ResponseEntity.noContent().build();
    }
}

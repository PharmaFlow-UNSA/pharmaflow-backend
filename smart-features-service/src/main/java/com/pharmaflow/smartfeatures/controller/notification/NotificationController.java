package com.pharmaflow.smartfeatures.controller.notification;

import com.pharmaflow.smartfeatures.dto.notification.NotificationDeliveryStatusRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationResponseDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationTriggerResponseDto;
import com.pharmaflow.smartfeatures.service.notification.NotificationService;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public ResponseEntity<List<NotificationResponseDto>> getNotifications(
      @RequestParam(required = false) @NullablePositive Long userId) {
    return ResponseEntity.ok(notificationService.getNotifications(userId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<NotificationResponseDto> getNotification(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(notificationService.getNotification(id));
  }

  @PostMapping
  public ResponseEntity<NotificationResponseDto> createNotification(
      @Valid @RequestBody NotificationRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(notificationService.createNotification(requestDto));
  }

  @PatchMapping("/{id}/delivery-status")
  public ResponseEntity<NotificationResponseDto> updateDeliveryStatus(
      @PathVariable @Positive Long id,
      @Valid @RequestBody NotificationDeliveryStatusRequestDto requestDto) {
    return ResponseEntity.ok(notificationService.updateDeliveryStatus(id, requestDto));
  }

  @PatchMapping("/{id}/mark-read")
  public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(notificationService.markAsRead(id));
  }

  @GetMapping("/{id}/triggers")
  public ResponseEntity<List<NotificationTriggerResponseDto>> getTriggers(
      @PathVariable @Positive Long id) {
    return ResponseEntity.ok(notificationService.getTriggers(id));
  }
}

package com.pharmaflow.smartfeatures.controller.notification;

import com.pharmaflow.smartfeatures.dto.notification.NotificationDeliveryStatusRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationRequestDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationResponseDto;
import com.pharmaflow.smartfeatures.dto.notification.NotificationTriggerResponseDto;
import com.pharmaflow.smartfeatures.security.AuthenticatedUsers;
import com.pharmaflow.smartfeatures.service.notification.NotificationService;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<List<NotificationResponseDto>> getNotifications(
      @RequestParam(required = false) @NullablePositive Long userId, Authentication authentication) {
    Authentication currentAuthentication = currentAuthentication(authentication);
    return ResponseEntity.ok(
        notificationService.getNotifications(
            userId,
            AuthenticatedUsers.from(currentAuthentication),
            AuthenticatedUsers.isAdmin(currentAuthentication)));
  }

  @GetMapping("/{id:-?\\d+}")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<NotificationResponseDto> getNotification(
      @PathVariable @Positive Long id, Authentication authentication) {
    Authentication currentAuthentication = currentAuthentication(authentication);
    return ResponseEntity.ok(
        notificationService.getNotification(
            id,
            AuthenticatedUsers.from(currentAuthentication),
            AuthenticatedUsers.isAdmin(currentAuthentication)));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<NotificationResponseDto> createNotification(
      @Valid @RequestBody NotificationRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(notificationService.createNotification(requestDto));
  }

  @PatchMapping("/{id:-?\\d+}/delivery-status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<NotificationResponseDto> updateDeliveryStatus(
      @PathVariable @Positive Long id,
      @Valid @RequestBody NotificationDeliveryStatusRequestDto requestDto) {
    return ResponseEntity.ok(notificationService.updateDeliveryStatus(id, requestDto));
  }

  @PatchMapping("/{id:-?\\d+}/mark-read")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<NotificationResponseDto> markAsRead(
      @PathVariable @Positive Long id, Authentication authentication) {
    Authentication currentAuthentication = currentAuthentication(authentication);
    return ResponseEntity.ok(
        notificationService.markAsRead(
            id,
            AuthenticatedUsers.from(currentAuthentication),
            AuthenticatedUsers.isAdmin(currentAuthentication)));
  }

  @GetMapping("/{id:-?\\d+}/triggers")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<List<NotificationTriggerResponseDto>> getTriggers(
      @PathVariable @Positive Long id, Authentication authentication) {
    Authentication currentAuthentication = currentAuthentication(authentication);
    return ResponseEntity.ok(
        notificationService.getTriggers(
            id,
            AuthenticatedUsers.from(currentAuthentication),
            AuthenticatedUsers.isAdmin(currentAuthentication)));
  }

  private Authentication currentAuthentication(Authentication authentication) {
    return authentication != null
        ? authentication
        : SecurityContextHolder.getContext().getAuthentication();
  }
}

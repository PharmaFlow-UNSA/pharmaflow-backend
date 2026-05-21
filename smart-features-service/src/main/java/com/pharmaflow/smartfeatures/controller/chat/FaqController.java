package com.pharmaflow.smartfeatures.controller.chat;

import com.pharmaflow.smartfeatures.dto.chat.FaqEntryRequestDto;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryResponseDto;
import com.pharmaflow.smartfeatures.service.chat.FaqService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/faqs")
@Tag(name = "FAQ")
public class FaqController {

  private final FaqService faqService;

  public FaqController(FaqService faqService) {
    this.faqService = faqService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<List<FaqEntryResponseDto>> getFaqEntries() {
    return ResponseEntity.ok(faqService.getFaqEntries());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<FaqEntryResponseDto> getFaqEntry(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(faqService.getFaqEntry(id));
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<List<FaqEntryResponseDto>> searchFaqEntries(
      @RequestParam
          @NotBlank(message = "query is required")
          @Size(min = 2, max = 200, message = "query must be between 2 and 200 characters")
          String query) {
    return ResponseEntity.ok(faqService.searchFaqEntries(query));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<FaqEntryResponseDto> createFaqEntry(
      @Valid @RequestBody FaqEntryRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(faqService.createFaqEntry(requestDto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<FaqEntryResponseDto> updateFaqEntry(
      @PathVariable @Positive Long id, @Valid @RequestBody FaqEntryRequestDto requestDto) {
    return ResponseEntity.ok(faqService.updateFaqEntry(id, requestDto));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteFaqEntry(@PathVariable @Positive Long id) {
    faqService.deleteFaqEntry(id);
    return ResponseEntity.noContent().build();
  }
}

package com.pharmaflow.smartfeatures.controller.chat;

import com.pharmaflow.smartfeatures.dto.PageResponseDto;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryResponseDto;
import com.pharmaflow.smartfeatures.service.chat.FaqService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/faqs")
@Tag(name = "FAQ Admin")
public class AdminFaqController {

  private final FaqService faqService;

  public AdminFaqController(FaqService faqService) {
    this.faqService = faqService;
  }

  @GetMapping("/page")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PageResponseDto<FaqEntryResponseDto>> getFaqEntriesPage(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(faqService.getFaqEntriesPage(page, size));
  }
}

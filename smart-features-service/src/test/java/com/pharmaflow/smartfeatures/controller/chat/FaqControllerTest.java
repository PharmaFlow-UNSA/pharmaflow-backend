package com.pharmaflow.smartfeatures.controller.chat;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.chat.FaqEntryResponseDto;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.service.chat.FaqService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FaqController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class FaqControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FaqService faqService;

  private FaqEntryResponseDto responseDto;

  @BeforeEach
  void setUp() {
    responseDto = new FaqEntryResponseDto();
    responseDto.setId(1L);
    responseDto.setQuestion("How to pay?");
    responseDto.setAnswer("Use card or cash.");
    responseDto.setCategory(FaqCategory.PAYMENTS);
    responseDto.setKeywords("pay");
    responseDto.setActive(true);
    responseDto.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void getFaqEntriesShouldReturn200() throws Exception {
    when(faqService.getFaqEntries()).thenReturn(List.of(responseDto));

    mockMvc
        .perform(get("/api/faqs").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].question").value("How to pay?"))
        .andExpect(jsonPath("$[0].isActive").value(true));
  }

  @Test
  void createFaqEntryShouldReturn201() throws Exception {
    when(faqService.createFaqEntry(any())).thenReturn(responseDto);

    mockMvc
        .perform(
            post("/api/faqs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "question": "How to pay?",
                                  "answer": "Use card or cash.",
                                  "category": "PAYMENTS",
                                  "keywords": "pay",
                                  "isActive": true
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.question").value("How to pay?"));
  }

  @Test
  void searchFaqEntriesShouldRejectShortQueryAtWebLayer() throws Exception {
    mockMvc
        .perform(
            get("/api/faqs/search").param("query", "a").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSTRAINT_VIOLATION"));

    verify(faqService, never()).searchFaqEntries(any());
  }
}

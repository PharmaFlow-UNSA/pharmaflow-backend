package com.pharmaflow.smartfeatures.controller.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.smartfeatures.dto.chat.FaqEntryRequestDto;
import com.pharmaflow.smartfeatures.dto.chatbot.ChatbotAskRequestDto;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("real-integration")
@EnabledIfEnvironmentVariable(named = "SMARTFEATURES_RUN_REAL_INTEGRATION_TESTS", matches = "true")
class ChatbotRealIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private DataSource dataSource;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void migrationShouldEnablePgvectorAndAddFaqEmbeddingColumn() throws Exception {
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'", Integer.class))
        .isEqualTo(1);

    try (Connection connection = dataSource.getConnection();
        ResultSet columns =
            connection.getMetaData().getColumns(null, null, "faq_entry", "embedding")) {
      assertThat(columns.next()).isTrue();
    }
  }

  @Test
  void seededFaqCorpusShouldContainRealWorldQuestions() throws Exception {
    Integer activeFaqCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM faq_entry WHERE is_active = TRUE", Integer.class);
    assertThat(activeFaqCount).isGreaterThanOrEqualTo(50);

    List<String> seededQuestions =
        List.of(
            "Can I upload my prescription as a PDF?",
            "Can I cancel an order before it is prepared?",
            "Can I change my delivery address?",
            "Where can I find my receipt?",
            "How do I update allergy information?");

    for (String question : seededQuestions) {
      Integer questionCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM faq_entry WHERE question = ?", Integer.class, question);
      assertThat(questionCount).isEqualTo(1);
    }

    mockMvc
        .perform(get("/api/faqs/search").param("query", "upload"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].category").value("PRESCRIPTIONS"));
  }

  @Test
  void chatbotAskShouldUseSeededFaqCorpusRealEmbeddingServiceAndDatabaseVectorSearch()
      throws Exception {
    mockMvc
        .perform(
            post("/api/chatbot/ask")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ChatbotAskRequestDto("Can I upload my prescription as a PDF?"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fallback").value(false))
        .andExpect(jsonPath("$.matchedQuestion").value("Can I upload my prescription as a PDF?"))
        .andExpect(jsonPath("$.category").value("PRESCRIPTIONS"))
        .andExpect(
            jsonPath("$.answer")
                .value(
                    "Yes. You can upload a prescription as a PDF or image, and the pharmacy team will review it before fulfillment."))
        .andExpect(jsonPath("$.confidence").isNumber());

    mockMvc
        .perform(
            post("/api/chatbot/ask")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ChatbotAskRequestDto("Where can I find my receipt?"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fallback").value(false))
        .andExpect(jsonPath("$.matchedQuestion").value("Where can I find my receipt?"))
        .andExpect(jsonPath("$.category").value("PAYMENTS"))
        .andExpect(jsonPath("$.confidence").isNumber());

    Integer embeddedRows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM faq_entry WHERE is_active = TRUE AND embedding IS NOT NULL",
            Integer.class);
    assertThat(embeddedRows).isGreaterThanOrEqualTo(50);
  }

  @Test
  void customFaqCreateShouldStoreEmbeddingAndParticipateInSearch() throws Exception {
    String suffix = UUID.randomUUID().toString();
    String question = "Can I upload my prescription as a PDF " + suffix + "?";
    String answer = "Yes. You can upload a prescription as a PDF or image for pharmacy review.";

    try {
      mockMvc
          .perform(
              post("/api/faqs")
                  .contentType("application/json")
                  .content(
                      objectMapper.writeValueAsString(
                          new FaqEntryRequestDto(
                              question,
                              answer,
                              FaqCategory.PRESCRIPTIONS,
                              "prescription,pdf,upload",
                              true))))
          .andExpect(status().isCreated());

      Integer embeddedRows =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM faq_entry WHERE question = ? AND embedding IS NOT NULL",
              Integer.class,
              question);
      assertThat(embeddedRows).isEqualTo(1);

      mockMvc
          .perform(
              post("/api/chatbot/ask")
                  .contentType("application/json")
                  .content(objectMapper.writeValueAsString(new ChatbotAskRequestDto(question))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.fallback").value(false))
          .andExpect(jsonPath("$.matchedQuestion").value(question))
          .andExpect(jsonPath("$.category").value("PRESCRIPTIONS"))
          .andExpect(jsonPath("$.answer").value(answer))
          .andExpect(jsonPath("$.confidence").isNumber());

      mockMvc
          .perform(
              post("/api/chatbot/ask")
                  .contentType("application/json")
                  .content(
                      objectMapper.writeValueAsString(
                          new ChatbotAskRequestDto("How do I repair a bicycle chain?"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.fallback").value(true))
          .andExpect(jsonPath("$.matchedQuestion").doesNotExist())
          .andExpect(jsonPath("$.confidence").value(0.0));
    } finally {
      jdbcTemplate.update("DELETE FROM faq_entry WHERE question = ?", question);
    }
  }
}

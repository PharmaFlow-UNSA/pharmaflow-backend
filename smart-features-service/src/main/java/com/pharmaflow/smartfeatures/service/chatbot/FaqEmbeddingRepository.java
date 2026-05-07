package com.pharmaflow.smartfeatures.service.chatbot;

import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FaqEmbeddingRepository {

  private final JdbcTemplate jdbcTemplate;

  public FaqEmbeddingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void updateEmbedding(Long faqId, List<Double> embedding) {
    jdbcTemplate.update(
        "UPDATE faq_entry SET embedding = CAST(? AS vector) WHERE faq_id = ?",
        toVectorLiteral(embedding),
        faqId);
  }

  public List<FaqEmbeddingCandidate> findActiveEntriesMissingEmbedding() {
    return jdbcTemplate.query(
        "SELECT faq_id, question FROM faq_entry WHERE is_active = TRUE AND embedding IS NULL",
        (resultSet, rowNum) ->
            new FaqEmbeddingCandidate(
                resultSet.getLong("faq_id"), resultSet.getString("question")));
  }

  public Optional<FaqSemanticMatch> findBestActiveMatch(List<Double> embedding) {
    List<FaqSemanticMatch> matches =
        jdbcTemplate.query(
            """
                SELECT faq_id, question, answer, category, 1 - (embedding <=> CAST(? AS vector)) AS similarity
                FROM faq_entry
                WHERE is_active = TRUE
                  AND embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT 1
                """,
            (resultSet, rowNum) ->
                new FaqSemanticMatch(
                    resultSet.getLong("faq_id"),
                    resultSet.getString("question"),
                    resultSet.getString("answer"),
                    FaqCategory.valueOf(resultSet.getString("category")),
                    resultSet.getDouble("similarity")),
            toVectorLiteral(embedding),
            toVectorLiteral(embedding));
    return matches.stream().findFirst();
  }

  private String toVectorLiteral(List<Double> embedding) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < embedding.size(); i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(embedding.get(i));
    }
    return builder.append(']').toString();
  }

  public record FaqEmbeddingCandidate(Long faqId, String question) {}

  public record FaqSemanticMatch(
      Long faqId, String question, String answer, FaqCategory category, Double similarity) {}
}

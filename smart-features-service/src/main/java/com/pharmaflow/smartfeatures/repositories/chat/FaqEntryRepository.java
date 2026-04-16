package com.pharmaflow.smartfeatures.repositories.chat;

import com.pharmaflow.smartfeatures.model.chat.FaqEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {

    List<FaqEntry> findAllByOrderByQuestionAsc();

    boolean existsByNormalizedQuestion(String normalizedQuestion);

    boolean existsByNormalizedQuestionAndFaqIdNot(String normalizedQuestion, Long faqId);

    @Query("""
            select f
            from FaqEntry f
            where f.isActive = true
              and (
                    lower(f.question) like lower(concat('%', :query, '%'))
                 or lower(f.answer) like lower(concat('%', :query, '%'))
                 or lower(coalesce(f.keywords, '')) like lower(concat('%', :query, '%'))
              )
            order by f.question asc
            """)
    List<FaqEntry> searchActive(@Param("query") String query);
}

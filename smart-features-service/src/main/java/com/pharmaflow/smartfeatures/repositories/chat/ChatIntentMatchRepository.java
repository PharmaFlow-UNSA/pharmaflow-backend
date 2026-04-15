package com.pharmaflow.smartfeatures.repositories.chat;

import com.pharmaflow.smartfeatures.model.chat.ChatIntentMatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatIntentMatchRepository extends JpaRepository<ChatIntentMatch, Long> {

    @EntityGraph(attributePaths = {"message", "faqEntry"})
    Optional<ChatIntentMatch> findByMessageMessageId(Long messageId);
}

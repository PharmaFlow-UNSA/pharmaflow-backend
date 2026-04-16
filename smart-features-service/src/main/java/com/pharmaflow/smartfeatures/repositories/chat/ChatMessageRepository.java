package com.pharmaflow.smartfeatures.repositories.chat;

import com.pharmaflow.smartfeatures.model.chat.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select message
            from ChatMessage message
            join fetch message.session session
            where session.sessionId = :sessionId
            order by message.createdAt asc
            """)
    List<ChatMessage> findBySessionSessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId);
}

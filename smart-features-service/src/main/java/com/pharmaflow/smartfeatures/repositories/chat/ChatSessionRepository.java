package com.pharmaflow.smartfeatures.repositories.chat;

import com.pharmaflow.smartfeatures.model.chat.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

  List<ChatSession> findByUserIdOrderByStartedAtDesc(Long userId);
}

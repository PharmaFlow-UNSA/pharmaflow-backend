package com.pharmaflow.smartfeatures.repositories.notification;

import com.pharmaflow.smartfeatures.model.notification.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  @EntityGraph(attributePaths = "therapyReminder")
  List<Notification> findAllByOrderByCreatedAtDesc();

  @EntityGraph(attributePaths = "therapyReminder")
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

  @Override
  @EntityGraph(attributePaths = "therapyReminder")
  Optional<Notification> findById(Long notificationId);
}

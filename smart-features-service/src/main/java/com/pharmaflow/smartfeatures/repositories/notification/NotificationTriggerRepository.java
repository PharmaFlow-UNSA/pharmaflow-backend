package com.pharmaflow.smartfeatures.repositories.notification;

import com.pharmaflow.smartfeatures.model.notification.NotificationTrigger;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationTriggerRepository extends JpaRepository<NotificationTrigger, Long> {

    @EntityGraph(attributePaths = "notification")
    List<NotificationTrigger> findByNotificationNotificationIdOrderByTriggeredAtDesc(Long notificationId);
}

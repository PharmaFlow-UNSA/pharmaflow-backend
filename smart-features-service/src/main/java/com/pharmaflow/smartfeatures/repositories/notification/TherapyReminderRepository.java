package com.pharmaflow.smartfeatures.repositories.notification;

import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import com.pharmaflow.smartfeatures.enums.notification.TherapyReminderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TherapyReminderRepository extends JpaRepository<TherapyReminder, Long> {

  List<TherapyReminder> findAllByOrderByStartDateDesc();

  List<TherapyReminder> findByPatientProfileIdOrderByStartDateDesc(Long patientProfileId);

  List<TherapyReminder> findByOwnerUserIdOrderByStartDateDesc(Long ownerUserId);

  List<TherapyReminder> findByOwnerUserIdAndPatientProfileIdOrderByStartDateDesc(
      Long ownerUserId, Long patientProfileId);

  List<TherapyReminder> findByStatusAndNextReminderAtLessThanEqualOrderByNextReminderAtAsc(
      TherapyReminderStatus status, LocalDateTime nextReminderAt);
}

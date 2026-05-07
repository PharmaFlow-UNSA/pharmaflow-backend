package com.pharmaflow.smartfeatures.repositories.notification;

import com.pharmaflow.smartfeatures.model.notification.TherapyReminder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TherapyReminderRepository extends JpaRepository<TherapyReminder, Long> {

  List<TherapyReminder> findAllByOrderByStartDateDesc();

  List<TherapyReminder> findByPatientProfileIdOrderByStartDateDesc(Long patientProfileId);
}

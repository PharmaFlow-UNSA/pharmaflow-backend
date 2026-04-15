package com.pharmaflow.smartfeatures.repositories.fraud;

import com.pharmaflow.smartfeatures.model.fraud.FraudCheck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {

    List<FraudCheck> findByUserIdOrderByCheckedAtDesc(Long userId);

    List<FraudCheck> findByOrderIdOrderByCheckedAtDesc(Long orderId);
}

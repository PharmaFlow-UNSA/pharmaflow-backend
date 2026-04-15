package com.pharmaflow.smartfeatures.repositories.fraud;

import com.pharmaflow.smartfeatures.model.fraud.FraudLog;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {

    @EntityGraph(attributePaths = {"fraudCheck", "fraudRule"})
    List<FraudLog> findByFraudCheckFraudCheckIdOrderByCreatedAtDesc(Long fraudCheckId);

    @EntityGraph(attributePaths = {"fraudCheck", "fraudRule"})
    List<FraudLog> findByFraudRuleRuleIdOrderByCreatedAtDesc(Long ruleId);
}

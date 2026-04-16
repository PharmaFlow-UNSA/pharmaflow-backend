package com.pharmaflow.smartfeatures.repositories.fraud;

import com.pharmaflow.smartfeatures.model.fraud.FraudRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    List<FraudRule> findAllByOrderByRuleNameAsc();

    boolean existsByNormalizedRuleName(String normalizedRuleName);

    boolean existsByNormalizedRuleNameAndRuleIdNot(String normalizedRuleName, Long ruleId);

    List<FraudRule> findByIsActiveTrueOrderByRuleNameAsc();
}

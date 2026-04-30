package guard.ics.backend.rule.repository;

import guard.ics.backend.rule.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RuleRepository extends JpaRepository<RuleEntity, Long>, JpaSpecificationExecutor<RuleEntity> {

    List<RuleEntity> findByEnabledTrueOrderByPriorityDesc();

    List<RuleEntity> findByRuleType(String ruleType);

    @Query("SELECT COALESCE(MAX(r.priority), 0) FROM RuleEntity r")
    int findMaxPriority();

    long countByEnabledTrue();
}

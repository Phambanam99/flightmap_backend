package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.AlertRule;
import com.phamnam.tracking_vessel_flight.models.enums.EntityType;
import com.phamnam.tracking_vessel_flight.models.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

        List<AlertRule> findByIsEnabledTrueOrderByPriorityDesc();

        List<AlertRule> findByIsEnabledTrueAndEntityTypeOrderByPriorityDesc(EntityType entityType);

        List<AlertRule> findByRuleType(RuleType ruleType);

        List<AlertRule> findByEntityType(EntityType entityType);

        List<AlertRule> findByPriority(AlertRule.Priority priority);

        Optional<AlertRule> findByNameAndEntityType(String name, EntityType entityType);

        @Query("SELECT ar FROM AlertRule ar WHERE ar.isEnabled = true AND ar.entityType = :entityType " +
                        "AND ar.ruleType = :ruleType ORDER BY ar.priority DESC")
        List<AlertRule> findActiveRulesByTypeAndEntity(@Param("entityType") EntityType entityType,
                        @Param("ruleType") RuleType ruleType);

        @Query("SELECT ar FROM AlertRule ar WHERE ar.isEnabled = true AND " +
                        "(ar.entityType = :entityType OR ar.entityType IS NULL) ORDER BY ar.priority DESC")
        List<AlertRule> findApplicableRules(@Param("entityType") EntityType entityType);

        @Query("SELECT COUNT(ar) FROM AlertRule ar WHERE ar.isEnabled = true AND ar.entityType = :entityType")
        long countActiveRulesByEntityType(@Param("entityType") EntityType entityType);

        boolean existsByNameAndEntityType(String name, EntityType entityType);
}
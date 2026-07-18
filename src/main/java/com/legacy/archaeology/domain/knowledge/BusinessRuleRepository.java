package com.legacy.archaeology.domain.knowledge;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, Long> {

    Optional<BusinessRuleEntity> findByBusinessRuleId(String businessRuleId);

    List<BusinessRuleEntity> findAllByProjectId(String projectId);

    List<BusinessRuleEntity> findAllByProjectIdAndReviewStatus(
            String projectId, ReviewStatus reviewStatus);

    List<BusinessRuleEntity> findAllByProjectIdAndConfidenceLevel(
            String projectId, ConfidenceLevel confidenceLevel);
}

package com.legacy.archaeology.domain.analysis;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJobEntity, Long> {

    Optional<AnalysisJobEntity> findByJobId(String jobId);

    List<AnalysisJobEntity> findAllByProjectIdOrderByCreatedAtDesc(String projectId);

    boolean existsByProjectIdAndJobTypeAndStatus(
            String projectId, JobType jobType, JobStatus status);
}

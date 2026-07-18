package com.legacy.archaeology.shared.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findAllByProjectIdOrderByTimestampDesc(String projectId);

    List<AuditLogEntity> findAllByJobIdOrderByTimestampDesc(String jobId);
}

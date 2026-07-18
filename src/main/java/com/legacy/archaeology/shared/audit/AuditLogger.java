package com.legacy.archaeology.shared.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 監査ログの記録を担当するコンポーネント。重要操作はすべてここを経由して記録する。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;

    public void log(
            String eventType,
            String projectId,
            String jobId,
            String userId,
            String targetType,
            String targetId,
            String details) {

        AuditLogEntity entry = new AuditLogEntity();
        entry.setEventType(eventType);
        entry.setProjectId(projectId);
        entry.setJobId(jobId);
        entry.setUserId(userId);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetails(details);

        auditLogRepository.save(entry);

        log.info(
                "AUDIT eventType={} projectId={} jobId={} userId={} targetType={} targetId={}",
                eventType,
                projectId,
                jobId,
                userId,
                targetType,
                targetId);
    }
}

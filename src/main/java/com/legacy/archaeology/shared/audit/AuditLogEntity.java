package com.legacy.archaeology.shared.audit;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 監査ログエンティティ。重要操作の証跡を保持する。 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "project_id", length = 20)
    private String projectId;

    @Column(name = "job_id", length = 20)
    private String jobId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @PrePersist
    void onCreate() {
        this.timestamp = OffsetDateTime.now();
    }
}

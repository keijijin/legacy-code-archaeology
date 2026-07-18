package com.legacy.archaeology.domain.knowledge;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 証拠エンティティ。
 * 解析結果・AI候補すべての根拠となるソースコード位置・文書断片を保持する。
 */
@Entity
@Table(name = "evidences")
@Getter
@Setter
@NoArgsConstructor
public class EvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_id", nullable = false, unique = true, length = 20)
    private String evidenceId;

    @Column(name = "project_id", nullable = false, length = 20)
    private String projectId;

    @Column(name = "source_asset_id", length = 20)
    private String sourceAssetId;

    @Column(name = "source_path", columnDefinition = "text")
    private String sourcePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "evidence_type", length = 50)
    private String evidenceType;

    @Column(name = "snippet", columnDefinition = "text")
    private String snippet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}

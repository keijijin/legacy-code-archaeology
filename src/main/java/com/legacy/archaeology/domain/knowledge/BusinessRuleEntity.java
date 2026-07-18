package com.legacy.archaeology.domain.knowledge;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 業務ルールエンティティ。
 * AI抽出候補および人間確認済みルールを保持する。
 * Inferred → Confirmed の自動遷移は禁止。
 */
@Entity
@Table(name = "business_rules")
@Getter
@Setter
@NoArgsConstructor
public class BusinessRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_rule_id", nullable = false, unique = true, length = 20)
    private String businessRuleId;

    @Column(name = "project_id", nullable = false, length = 20)
    private String projectId;

    @Column(name = "rule_text", columnDefinition = "text", nullable = false)
    private String ruleText;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 20)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus;

    @ElementCollection
    @CollectionTable(
            name = "business_rule_evidence_links",
            joinColumns = @JoinColumn(name = "business_rule_id", referencedColumnName = "business_rule_id"))
    @Column(name = "evidence_id")
    private List<String> evidenceIds;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.reviewStatus == null) {
            this.reviewStatus = ReviewStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * レビューによる状態昇格。
     * INFERRED → CONFIRMED への自動遷移は禁止。
     */
    public void approve(String reviewComment) {
        if (this.reviewStatus == ReviewStatus.REJECTED) {
            throw new IllegalStateException("却下済みのルールは直接承認できません");
        }
        this.reviewStatus = ReviewStatus.APPROVED;
        this.confidenceLevel = ConfidenceLevel.CONFIRMED;
    }

    public void reject() {
        this.reviewStatus = ReviewStatus.REJECTED;
    }

    public void putOnHold() {
        this.reviewStatus = ReviewStatus.ON_HOLD;
    }
}

package com.legacy.archaeology.domain.reviews;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * レビューエンティティ。
 * 人間レビュー担当による承認・却下・修正履歴を保持する。
 * PENDING → APPROVED は人間レビューでのみ付与可能。
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false, unique = true, length = 20)
    private String reviewId;

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 50)
    private String targetId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "reviewer_id", length = 100)
    private String reviewerId;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private OffsetDateTime reviewedAt;

    @PrePersist
    void onCreate() {
        this.reviewedAt = OffsetDateTime.now();
    }
}

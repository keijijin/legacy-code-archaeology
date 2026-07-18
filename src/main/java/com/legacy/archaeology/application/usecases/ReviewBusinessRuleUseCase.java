package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.ReviewDto;
import com.legacy.archaeology.domain.knowledge.BusinessRuleRepository;
import com.legacy.archaeology.domain.reviews.ReviewEntity;
import com.legacy.archaeology.domain.reviews.ReviewRepository;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 業務ルールレビューユースケース。
 * INFERRED → CONFIRMED の自動昇格は禁止。
 * APPROVED は人間レビューでのみ付与可能。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewBusinessRuleUseCase {

    private final BusinessRuleRepository businessRuleRepository;
    private final ReviewRepository reviewRepository;
    private final IdGenerator idGenerator;
    private final AuditLogger auditLogger;

    @Transactional
    public ReviewDto.Response execute(
            String projectId,
            String businessRuleId,
            ReviewDto.ReviewRequest request,
            String reviewerId) {

        var rule = businessRuleRepository
                .findByBusinessRuleId(businessRuleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "業務ルールが見つかりません: " + businessRuleId));

        switch (request.getAction()) {
            case "Approved" -> rule.approve(request.getComment());
            case "Rejected" -> rule.reject();
            case "OnHold"   -> rule.putOnHold();
            default -> throw new IllegalArgumentException(
                    "不正なレビューアクション: " + request.getAction());
        }
        businessRuleRepository.save(rule);

        ReviewEntity review = new ReviewEntity();
        review.setReviewId(idGenerator.generateReviewId());
        review.setTargetType("BusinessRule");
        review.setTargetId(businessRuleId);
        review.setAction(request.getAction());
        review.setComment(request.getComment());
        review.setReviewerId(reviewerId);
        reviewRepository.save(review);

        auditLogger.log(
                "BUSINESS_RULE_REVIEWED",
                projectId,
                null,
                reviewerId,
                "BusinessRule",
                businessRuleId,
                request.getAction());

        log.info("業務ルールレビュー完了 ruleId={} action={} reviewer={}",
                businessRuleId, request.getAction(), reviewerId);

        return ReviewDto.Response.builder()
                .reviewId(review.getReviewId())
                .targetType("BusinessRule")
                .targetId(businessRuleId)
                .action(request.getAction())
                .reviewStatus(rule.getReviewStatus().name())
                .confidenceLevel(rule.getConfidenceLevel().name())
                .reviewedAt(review.getReviewedAt())
                .build();
    }
}

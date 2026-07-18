package com.legacy.archaeology.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

/** レビュー関連 DTO */
public class ReviewDto {

    @Getter
    @Builder
    public static class ReviewRequest {
        @NotBlank(message = "アクションは必須です (Approved / Rejected / OnHold)")
        private String action;

        private String comment;
    }

    @Getter
    @Builder
    public static class Response {
        private String reviewId;
        private String targetType;
        private String targetId;
        private String action;
        private String reviewStatus;
        private String confidenceLevel;
        private OffsetDateTime reviewedAt;
    }
}

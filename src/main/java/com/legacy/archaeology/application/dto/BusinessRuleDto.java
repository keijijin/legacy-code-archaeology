package com.legacy.archaeology.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 業務ルール関連 DTO */
public class BusinessRuleDto {

    @Getter
    @Builder
    public static class Response {
        private String businessRuleId;
        private String projectId;
        private String ruleText;
        private Confidence confidence;
        private String reviewStatus;
        private List<String> evidenceIds;
        private String reason;
        private String modelName;
        private String promptVersion;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class Confidence {
        private String level;
        private Double score;
    }
}

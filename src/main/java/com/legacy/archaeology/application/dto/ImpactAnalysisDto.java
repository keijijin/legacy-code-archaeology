package com.legacy.archaeology.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 影響分析 DTO */
public class ImpactAnalysisDto {

    @Getter
    @Builder
    public static class Request {
        /** COLUMN / TABLE / ROUTE / PROGRAM / ENDPOINT / API */
        @NotBlank
        private String targetType;

        /** ノードID（ENT-xxxx 等）。name とどちらか必須 */
        private String targetId;

        /** 名前検索（カラム名、routeId、className 等） */
        private String targetName;

        @Min(1)
        @Max(5)
        @Builder.Default
        private Integer maxDepth = 3;
    }

    @Getter
    @Builder
    public static class Response {
        private String projectId;
        private String targetType;
        private String targetId;
        private String targetName;
        private int impactedCount;
        private List<ImpactedNode> impactedNodes;
        private List<RelatedTest> relatedTests;
        private List<String> evidenceSummary;
    }

    @Getter
    @Builder
    public static class ImpactedNode {
        private String nodeType;
        private String nodeId;
        private String name;
        private int depth;
        private String impactLevel;
        private List<String> relationPath;
        private String sourcePath;
    }

    @Getter
    @Builder
    public static class RelatedTest {
        private String testId;
        private String testName;
        private String sourcePath;
        private String verifiedTargetId;
        private String verifiedTargetName;
    }
}

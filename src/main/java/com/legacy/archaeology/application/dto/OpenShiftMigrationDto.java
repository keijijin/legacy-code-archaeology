package com.legacy.archaeology.application.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** OpenShift 移行課題 DTO */
public class OpenShiftMigrationDto {

    @Getter
    @Builder
    public static class Response {
        private String projectId;
        private int issueCount;
        private List<Issue> issues;
    }

    @Getter
    @Builder
    public static class Issue {
        private String issueId;
        private String category;
        private String severity;
        private String title;
        private String description;
        private String recommendation;
        private List<String> evidenceIds;
        private String sourcePath;
        private String confidenceLevel;
    }
}

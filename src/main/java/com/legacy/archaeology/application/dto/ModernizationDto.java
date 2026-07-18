package com.legacy.archaeology.application.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** モダナイゼーション候補 DTO */
public class ModernizationDto {

    @Getter
    @Builder
    public static class Response {
        private String projectId;
        private int candidateCount;
        private List<Candidate> candidates;
    }

    @Getter
    @Builder
    public static class Candidate {
        private String candidateId;
        private String targetType;
        private String targetId;
        private String targetName;
        /** KEEP / RETIRE / REDESIGN / APIZE / EVENTIZE / REPLATFORM */
        private String action;
        private String rationale;
        private String priority;
        private String confidenceLevel;
        private List<String> evidenceIds;
        private List<String> relatedNodeIds;
    }
}

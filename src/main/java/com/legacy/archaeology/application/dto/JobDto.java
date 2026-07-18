package com.legacy.archaeology.application.dto;

import com.legacy.archaeology.domain.analysis.JobStatus;
import com.legacy.archaeology.domain.analysis.JobType;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

/** 解析ジョブ関連 DTO */
public class JobDto {

    @Getter
    @Builder
    public static class AnalyzeRequest {
        @NotNull(message = "ジョブ種別は必須です")
        private JobType jobType;

        private boolean forceFullReanalysis;
    }

    @Getter
    @Builder
    public static class Response {
        private String jobId;
        private String projectId;
        private JobType jobType;
        private JobStatus status;
        private String requestedBy;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private String errorCode;
        private String errorMessage;
        private OffsetDateTime createdAt;
    }
}

package com.legacy.archaeology.application.dto;

import com.legacy.archaeology.domain.projects.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

/** プロジェクト関連 DTO */
public class ProjectDto {

    @Getter
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "プロジェクト名は必須です")
        @Size(max = 255)
        private String name;

        @Size(max = 2000)
        private String description;
    }

    @Getter
    @Builder
    public static class Response {
        private String projectId;
        private String name;
        private String description;
        private ProjectStatus status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
}

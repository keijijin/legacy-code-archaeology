package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.ProjectDto;
import com.legacy.archaeology.domain.projects.ProjectEntity;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** プロジェクト作成ユースケース */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateProjectUseCase {

    private final ProjectRepository projectRepository;
    private final IdGenerator idGenerator;
    private final AuditLogger auditLogger;

    @Transactional
    public ProjectDto.Response execute(ProjectDto.CreateRequest request, String userId) {
        String projectId = idGenerator.generateProjectId();

        ProjectEntity entity = new ProjectEntity();
        entity.setProjectId(projectId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        projectRepository.save(entity);

        auditLogger.log(
                "PROJECT_CREATED", projectId, null, userId, "Project", projectId, request.getName());

        log.info("プロジェクト作成完了 projectId={} name={}", projectId, request.getName());

        return toResponse(entity);
    }

    public static ProjectDto.Response toResponse(ProjectEntity entity) {
        return ProjectDto.Response.builder()
                .projectId(entity.getProjectId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

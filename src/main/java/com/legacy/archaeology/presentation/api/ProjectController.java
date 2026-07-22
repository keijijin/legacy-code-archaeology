package com.legacy.archaeology.presentation.api;

import com.legacy.archaeology.application.dto.AssetDto;
import com.legacy.archaeology.application.dto.JobDto;
import com.legacy.archaeology.application.dto.ProjectDto;
import com.legacy.archaeology.application.usecases.CreateProjectUseCase;
import com.legacy.archaeology.application.usecases.IngestAssetUseCase;
import com.legacy.archaeology.application.usecases.SubmitAnalysisJobUseCase;
import com.legacy.archaeology.domain.analysis.AnalysisJobRepository;
import com.legacy.archaeology.domain.assets.AssetRepository;
import com.legacy.archaeology.domain.knowledge.BusinessRuleRepository;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** プロジェクト管理 REST API */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final IngestAssetUseCase ingestAssetUseCase;
    private final SubmitAnalysisJobUseCase submitAnalysisJobUseCase;
    private final ProjectRepository projectRepository;
    private final AssetRepository assetRepository;
    private final BusinessRuleRepository businessRuleRepository;
    private final AnalysisJobRepository analysisJobRepository;

    /** プロジェクト作成 */
    @PostMapping
    public ResponseEntity<ProjectDto.Response> createProject(
            @RequestBody @Valid ProjectDto.CreateRequest request) {
        ProjectDto.Response response = createProjectUseCase.execute(request, "system");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** プロジェクト一覧取得 */
    @GetMapping
    public ResponseEntity<List<ProjectDto.Response>> listProjects() {
        List<ProjectDto.Response> list =
                projectRepository.findAll().stream()
                        .map(CreateProjectUseCase::toResponse)
                        .toList();
        return ResponseEntity.ok(list);
    }

    /** プロジェクト取得 */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto.Response> getProject(@PathVariable String projectId) {
        return projectRepository
                .findByProjectId(projectId)
                .map(CreateProjectUseCase::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** プロジェクトサマリ取得 */
    @GetMapping("/{projectId}/summary")
    public ResponseEntity<java.util.Map<String, Object>> summary(@PathVariable String projectId) {
        long assetCount = assetRepository.findAllByProjectId(projectId).size();
        long ruleCount = businessRuleRepository.findAllByProjectId(projectId).size();
        java.util.List<JobDto.Response> jobs = analysisJobRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(IngestAssetUseCase::toJobResponse)
                .toList();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("assetCount", assetCount);
        body.put("ruleCount", ruleCount);
        body.put("jobCount", jobs.size());
        body.put("latestJobStatus", jobs.isEmpty() ? null : jobs.get(0).getStatus());
        return ResponseEntity.ok(body);
    }

    /** 資産取込 */
    @PostMapping("/{projectId}/ingest")
    public ResponseEntity<JobDto.Response> ingest(
            @PathVariable String projectId,
            @RequestBody @Valid AssetDto.IngestRequest request) {
        JobDto.Response response = ingestAssetUseCase.execute(projectId, request, "system");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /** 解析ジョブ投入 */
    @PostMapping("/{projectId}/analyze")
    public ResponseEntity<JobDto.Response> analyze(
            @PathVariable String projectId,
            @RequestBody @Valid JobDto.AnalyzeRequest request) {
        JobDto.Response response = submitAnalysisJobUseCase.execute(projectId, request, "system");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

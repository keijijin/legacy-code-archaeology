package com.legacy.archaeology.presentation.api;

import com.legacy.archaeology.application.dto.JobDto;
import com.legacy.archaeology.application.usecases.IngestAssetUseCase;
import com.legacy.archaeology.domain.analysis.AnalysisJobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 解析ジョブ参照 REST API */
@RestController
@RequestMapping("/api/projects/{projectId}/jobs")
@RequiredArgsConstructor
public class JobController {

    private final AnalysisJobRepository jobRepository;

    /** ジョブ一覧取得 */
    @GetMapping
    public ResponseEntity<List<JobDto.Response>> listJobs(@PathVariable String projectId) {
        List<JobDto.Response> list =
                jobRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
                        .map(IngestAssetUseCase::toJobResponse)
                        .toList();
        return ResponseEntity.ok(list);
    }

    /** ジョブ取得 */
    @GetMapping("/{jobId}")
    public ResponseEntity<JobDto.Response> getJob(
            @PathVariable String projectId, @PathVariable String jobId) {
        return jobRepository
                .findByJobId(jobId)
                .map(IngestAssetUseCase::toJobResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

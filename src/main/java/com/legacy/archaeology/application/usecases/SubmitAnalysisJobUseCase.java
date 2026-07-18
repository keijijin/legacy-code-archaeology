package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.JobDto;
import com.legacy.archaeology.domain.analysis.AnalysisJobEntity;
import com.legacy.archaeology.domain.analysis.AnalysisJobRepository;
import com.legacy.archaeology.domain.analysis.JobStatus;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 解析ジョブ投入ユースケース */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitAnalysisJobUseCase {

    private final AnalysisJobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final IdGenerator idGenerator;
    private final AuditLogger auditLogger;

    @Transactional
    public JobDto.Response execute(
            String projectId, JobDto.AnalyzeRequest request, String userId) {

        projectRepository
                .findByProjectId(projectId)
                .orElseThrow(
                        () -> new IllegalArgumentException("プロジェクトが見つかりません: " + projectId));

        boolean alreadyRunning =
                jobRepository.existsByProjectIdAndJobTypeAndStatus(
                        projectId, request.getJobType(), JobStatus.RUNNING);
        if (alreadyRunning) {
            throw new IllegalStateException(
                    "同種の解析ジョブが実行中です: " + request.getJobType());
        }

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setJobId(idGenerator.generateJobId());
        job.setProjectId(projectId);
        job.setJobType(request.getJobType());
        job.setStatus(JobStatus.QUEUED);
        job.setRequestedBy(userId);
        jobRepository.save(job);

        auditLogger.log(
                "ANALYSIS_JOB_SUBMITTED",
                projectId,
                job.getJobId(),
                userId,
                "AnalysisJob",
                job.getJobId(),
                request.getJobType().name());

        log.info("解析ジョブ投入完了 jobId={} type={} projectId={}",
                job.getJobId(), request.getJobType(), projectId);

        return IngestAssetUseCase.toJobResponse(job);
    }
}

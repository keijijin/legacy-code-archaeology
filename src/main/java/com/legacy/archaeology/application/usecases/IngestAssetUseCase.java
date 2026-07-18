package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.AssetDto;
import com.legacy.archaeology.application.dto.JobDto;
import com.legacy.archaeology.domain.analysis.AnalysisJobEntity;
import com.legacy.archaeology.domain.analysis.AnalysisJobRepository;
import com.legacy.archaeology.domain.analysis.JobStatus;
import com.legacy.archaeology.domain.analysis.JobType;
import com.legacy.archaeology.domain.assets.AssetEntity;
import com.legacy.archaeology.domain.assets.AssetRepository;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 資産取込ユースケース */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestAssetUseCase {

    private final AssetRepository assetRepository;
    private final AnalysisJobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final IdGenerator idGenerator;
    private final AuditLogger auditLogger;

    @Transactional
    public JobDto.Response execute(
            String projectId, AssetDto.IngestRequest request, String userId) {

        projectRepository
                .findByProjectId(projectId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "プロジェクトが見つかりません: " + projectId));

        String versionHash = computeHash(request.getSourcePath());

        AssetEntity asset = new AssetEntity();
        asset.setAssetId(idGenerator.generateAssetId());
        asset.setProjectId(projectId);
        asset.setAssetType(request.getAssetType());
        asset.setSourcePath(request.getSourcePath());
        asset.setVersionHash(versionHash);
        assetRepository.save(asset);

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setJobId(idGenerator.generateJobId());
        job.setProjectId(projectId);
        job.setJobType(JobType.INGESTION);
        job.setStatus(JobStatus.QUEUED);
        job.setRequestedBy(userId);
        jobRepository.save(job);

        auditLogger.log(
                "ASSET_INGESTED",
                projectId,
                job.getJobId(),
                userId,
                "Asset",
                asset.getAssetId(),
                request.getSourcePath());

        log.info(
                "資産取込完了 assetId={} jobId={} projectId={}",
                asset.getAssetId(),
                job.getJobId(),
                projectId);

        return toJobResponse(job);
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

    public static JobDto.Response toJobResponse(AnalysisJobEntity job) {
        return JobDto.Response.builder()
                .jobId(job.getJobId())
                .projectId(job.getProjectId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .requestedBy(job.getRequestedBy())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .errorCode(job.getErrorCode())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .build();
    }
}

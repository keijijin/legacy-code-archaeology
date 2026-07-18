package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.ModernizationDto;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.infrastructure.graph.ImpactGraphQueryService;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * モダナイゼーション候補生成ユースケース。
 * 結合度・接続形態から KEEP/RETIRE/REDESIGN/APIZE/EVENTIZE 等を候補化する。
 * 最終確定は人間レビュー前提。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateModernizationCandidatesUseCase {

    private final ProjectRepository projectRepository;
    private final ImpactGraphQueryService impactGraphQueryService;
    private final IdGenerator idGenerator;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public ModernizationDto.Response execute(String projectId, String userId) {
        projectRepository
                .findByProjectId(projectId)
                .orElseThrow(
                        () -> new IllegalArgumentException("プロジェクトが見つかりません: " + projectId));

        List<Map<String, Object>> targets =
                impactGraphQueryService.findModernizationTargets(projectId);
        List<ModernizationDto.Candidate> candidates = new ArrayList<>();

        for (Map<String, Object> target : targets) {
            candidates.add(toCandidate(target));
        }

        auditLogger.log(
                "MODERNIZATION_CANDIDATES_GENERATED",
                projectId,
                null,
                userId,
                "ModernizationPlan",
                projectId,
                "candidateCount=" + candidates.size());

        return ModernizationDto.Response.builder()
                .projectId(projectId)
                .candidateCount(candidates.size())
                .candidates(candidates)
                .build();
    }

    private ModernizationDto.Candidate toCandidate(Map<String, Object> target) {
        String nodeType = String.valueOf(target.getOrDefault("nodeType", "Unknown"));
        String nodeId = String.valueOf(target.getOrDefault("nodeId", ""));
        String name = String.valueOf(target.getOrDefault("name", ""));
        String sourcePath = String.valueOf(target.getOrDefault("sourcePath", ""));
        String fromUri = String.valueOf(target.getOrDefault("fromUri", "")).toLowerCase(Locale.ROOT);
        int degree = ((Number) target.getOrDefault("degree", 0)).intValue();

        String action;
        String rationale;
        String priority;
        String confidence;

        if (degree == 0) {
            action = "RETIRE";
            rationale = "参照関係が検出されないため、廃止候補として提示";
            priority = "MEDIUM";
            confidence = "INFERRED";
        } else if (degree >= 8) {
            action = "REDESIGN";
            rationale = "高結合（degree=" + degree + "）のため再設計候補";
            priority = "HIGH";
            confidence = "LIKELY";
        } else if (fromUri.startsWith("jms:") || fromUri.contains("queue") || fromUri.contains("kafka")) {
            action = "EVENTIZE";
            rationale = "メッセージ連携中心のためイベント駆動化候補";
            priority = "MEDIUM";
            confidence = "LIKELY";
        } else if ("Route".equalsIgnoreCase(nodeType) || fromUri.startsWith("http")) {
            action = "APIZE";
            rationale = "同期連携/Route中心のため API 境界整理候補";
            priority = "MEDIUM";
            confidence = "LIKELY";
        } else if ("Table".equalsIgnoreCase(nodeType) && degree >= 4) {
            action = "REPLATFORM";
            rationale = "共有テーブル結合が強いためデータ再配置/再プラットフォーム候補";
            priority = "HIGH";
            confidence = "INFERRED";
        } else {
            action = "KEEP";
            rationale = "結合度が中程度で当面維持候補";
            priority = "LOW";
            confidence = "INFERRED";
        }

        return ModernizationDto.Candidate.builder()
                .candidateId(idGenerator.generateBusinessRuleId().replace("BR-", "MD-"))
                .targetType(nodeType)
                .targetId(nodeId)
                .targetName(name)
                .action(action)
                .rationale(rationale + (sourcePath.isBlank() ? "" : " source=" + sourcePath))
                .priority(priority)
                .confidenceLevel(confidence)
                .evidenceIds(List.of(nodeId))
                .relatedNodeIds(List.of(nodeId))
                .build();
    }
}

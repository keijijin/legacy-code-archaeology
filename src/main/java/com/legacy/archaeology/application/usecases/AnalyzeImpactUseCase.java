package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.ImpactAnalysisDto;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.infrastructure.graph.ImpactGraphQueryService;
import com.legacy.archaeology.shared.audit.AuditLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 影響分析ユースケース。
 * DBカラム / API / Route / Program 変更時の影響範囲を知識グラフから抽出する。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeImpactUseCase {

    private final ProjectRepository projectRepository;
    private final ImpactGraphQueryService impactGraphQueryService;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public ImpactAnalysisDto.Response execute(
            String projectId, ImpactAnalysisDto.Request request, String userId) {

        projectRepository
                .findByProjectId(projectId)
                .orElseThrow(
                        () -> new IllegalArgumentException("プロジェクトが見つかりません: " + projectId));

        if ((request.getTargetId() == null || request.getTargetId().isBlank())
                && (request.getTargetName() == null || request.getTargetName().isBlank())) {
            throw new IllegalArgumentException("targetId または targetName のいずれかは必須です");
        }

        int maxDepth = request.getMaxDepth() == null ? 3 : request.getMaxDepth();

        Map<String, Object> start =
                impactGraphQueryService
                        .findStartNode(
                                projectId,
                                request.getTargetType(),
                                request.getTargetId(),
                                request.getTargetName())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "影響分析の起点ノードが見つかりません: type="
                                                        + request.getTargetType()
                                                        + ", id="
                                                        + request.getTargetId()
                                                        + ", name="
                                                        + request.getTargetName()));

        String startId = String.valueOf(start.get("nodeId"));
        List<Map<String, Object>> impactedRows =
                impactGraphQueryService.findImpactedNodes(projectId, startId, maxDepth);
        List<Map<String, Object>> testRows =
                impactGraphQueryService.findRelatedTests(projectId, startId);

        List<ImpactAnalysisDto.ImpactedNode> impactedNodes = new ArrayList<>();
        List<String> evidenceSummary = new ArrayList<>();
        evidenceSummary.add(
                "start="
                        + start.get("nodeType")
                        + ":"
                        + start.get("name")
                        + " sourcePath="
                        + start.get("sourcePath"));

        for (Map<String, Object> row : impactedRows) {
            @SuppressWarnings("unchecked")
            List<String> relationPath =
                    row.get("relationPath") instanceof List<?> list
                            ? list.stream().map(String::valueOf).toList()
                            : List.of();
            int depth = ((Number) row.getOrDefault("depth", 0)).intValue();
            String level = impactGraphQueryService.resolveImpactLevel(depth, relationPath);

            impactedNodes.add(
                    ImpactAnalysisDto.ImpactedNode.builder()
                            .nodeType(String.valueOf(row.get("nodeType")))
                            .nodeId(String.valueOf(row.get("nodeId")))
                            .name(String.valueOf(row.get("name")))
                            .depth(depth)
                            .impactLevel(level)
                            .relationPath(relationPath)
                            .sourcePath(String.valueOf(row.getOrDefault("sourcePath", "")))
                            .build());

            if (evidenceSummary.size() < 20) {
                evidenceSummary.add(
                        row.get("nodeType")
                                + ":"
                                + row.get("name")
                                + " depth="
                                + depth
                                + " level="
                                + level);
            }
        }

        List<ImpactAnalysisDto.RelatedTest> relatedTests =
                testRows.stream()
                        .map(
                                row ->
                                        ImpactAnalysisDto.RelatedTest.builder()
                                                .testId(String.valueOf(row.get("testId")))
                                                .testName(String.valueOf(row.get("testName")))
                                                .sourcePath(
                                                        String.valueOf(
                                                                row.getOrDefault("sourcePath", "")))
                                                .verifiedTargetId(
                                                        String.valueOf(row.get("verifiedTargetId")))
                                                .verifiedTargetName(
                                                        String.valueOf(
                                                                row.get("verifiedTargetName")))
                                                .build())
                        .toList();

        auditLogger.log(
                "IMPACT_ANALYSIS_EXECUTED",
                projectId,
                null,
                userId,
                request.getTargetType(),
                startId,
                "impacted=" + impactedNodes.size() + " tests=" + relatedTests.size());

        log.info(
                "影響分析完了 projectId={} target={} impacted={} tests={}",
                projectId,
                startId,
                impactedNodes.size(),
                relatedTests.size());

        return ImpactAnalysisDto.Response.builder()
                .projectId(projectId)
                .targetType(String.valueOf(start.get("nodeType")))
                .targetId(startId)
                .targetName(String.valueOf(start.get("name")))
                .impactedCount(impactedNodes.size())
                .impactedNodes(impactedNodes)
                .relatedTests(relatedTests)
                .evidenceSummary(evidenceSummary)
                .build();
    }
}

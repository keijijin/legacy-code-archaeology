package com.legacy.archaeology.presentation.api;

import com.legacy.archaeology.application.dto.ImpactAnalysisDto;
import com.legacy.archaeology.application.dto.ModernizationDto;
import com.legacy.archaeology.application.dto.OpenShiftMigrationDto;
import com.legacy.archaeology.application.usecases.AnalyzeImpactUseCase;
import com.legacy.archaeology.application.usecases.ExtractOpenShiftMigrationIssuesUseCase;
import com.legacy.archaeology.application.usecases.GenerateModernizationCandidatesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 影響分析・移行支援 REST API */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ImpactAnalysisController {

    private final AnalyzeImpactUseCase analyzeImpactUseCase;
    private final ExtractOpenShiftMigrationIssuesUseCase extractOpenShiftMigrationIssuesUseCase;
    private final GenerateModernizationCandidatesUseCase generateModernizationCandidatesUseCase;

    /**
     * 影響分析。
     * targetType=COLUMN/TABLE/ROUTE/PROGRAM/ENDPOINT/API
     */
    @PostMapping("/impact")
    public ResponseEntity<ImpactAnalysisDto.Response> analyzeImpact(
            @PathVariable String projectId,
            @RequestBody @Valid ImpactAnalysisDto.Request request) {
        return ResponseEntity.ok(analyzeImpactUseCase.execute(projectId, request, "system"));
    }

    /** 関連テスト抽出（影響分析レスポンス内でも返すが、単体でも取得可能） */
    @GetMapping("/impact/tests")
    public ResponseEntity<ImpactAnalysisDto.Response> relatedTests(
            @PathVariable String projectId,
            @RequestParam String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String targetName) {
        ImpactAnalysisDto.Request request =
                ImpactAnalysisDto.Request.builder()
                        .targetType(targetType)
                        .targetId(targetId)
                        .targetName(targetName)
                        .maxDepth(3)
                        .build();
        return ResponseEntity.ok(analyzeImpactUseCase.execute(projectId, request, "system"));
    }

    /** OpenShift 移行課題抽出 */
    @GetMapping("/openshift-migration-issues")
    public ResponseEntity<OpenShiftMigrationDto.Response> openShiftIssues(
            @PathVariable String projectId) {
        return ResponseEntity.ok(
                extractOpenShiftMigrationIssuesUseCase.execute(projectId, "system"));
    }

    /** モダナイゼーション候補生成 */
    @GetMapping("/modernization-plan")
    public ResponseEntity<ModernizationDto.Response> modernizationPlan(
            @PathVariable String projectId) {
        return ResponseEntity.ok(
                generateModernizationCandidatesUseCase.execute(projectId, "system"));
    }
}

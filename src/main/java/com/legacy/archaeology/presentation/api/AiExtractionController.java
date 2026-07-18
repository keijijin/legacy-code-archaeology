package com.legacy.archaeology.presentation.api;

import com.legacy.archaeology.application.usecases.ExtractBusinessRuleCandidatesUseCase;
import com.legacy.archaeology.application.usecases.ExtractMismatchCandidatesUseCase;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** AI候補抽出 REST API */
@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
public class AiExtractionController {

    private final ExtractBusinessRuleCandidatesUseCase extractBusinessRuleUseCase;
    private final ExtractMismatchCandidatesUseCase extractMismatchUseCase;

    /**
     * 業務ルール候補抽出ジョブ起動。
     * 非同期処理前提：受付のみ返す。
     */
    @PostMapping("/extract-rules")
    public ResponseEntity<Map<String, Object>> extractRules(
            @PathVariable String projectId) {

        List<String> ruleIds = extractBusinessRuleUseCase.execute(projectId, null, "system");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "Accepted",
                "extractedCount", ruleIds.size(),
                "businessRuleIds", ruleIds));
    }

    /**
     * 設計書と実装の不一致候補抽出ジョブ起動。
     */
    @PostMapping("/extract-mismatches")
    public ResponseEntity<Map<String, Object>> extractMismatches(
            @PathVariable String projectId) {

        List<String> ids = extractMismatchUseCase.execute(projectId, null, "system");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "Accepted",
                "extractedCount", ids.size(),
                "candidateIds", ids));
    }
}

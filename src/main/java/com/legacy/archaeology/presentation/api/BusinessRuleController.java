package com.legacy.archaeology.presentation.api;

import com.legacy.archaeology.application.dto.BusinessRuleDto;
import com.legacy.archaeology.application.dto.ReviewDto;
import com.legacy.archaeology.application.usecases.ReviewBusinessRuleUseCase;
import com.legacy.archaeology.domain.knowledge.BusinessRuleRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 業務ルール API */
@RestController
@RequestMapping("/api/projects/{projectId}/rules")
@RequiredArgsConstructor
public class BusinessRuleController {

    private final BusinessRuleRepository businessRuleRepository;
    private final ReviewBusinessRuleUseCase reviewBusinessRuleUseCase;

    /** 業務ルール一覧取得 */
    @GetMapping
    public ResponseEntity<List<BusinessRuleDto.Response>> listRules(
            @PathVariable String projectId) {
        List<BusinessRuleDto.Response> list =
                businessRuleRepository.findAllByProjectId(projectId).stream()
                        .map(r -> BusinessRuleDto.Response.builder()
                                .businessRuleId(r.getBusinessRuleId())
                                .projectId(r.getProjectId())
                                .ruleText(r.getRuleText())
                                .confidence(BusinessRuleDto.Confidence.builder()
                                        .level(r.getConfidenceLevel().name())
                                        .score(r.getConfidenceScore() == null
                                                ? null
                                                : r.getConfidenceScore().doubleValue())
                                        .build())
                                .reviewStatus(r.getReviewStatus().name())
                                .evidenceIds(r.getEvidenceIds())
                                .reason(r.getReason())
                                .modelName(r.getModelName())
                                .promptVersion(r.getPromptVersion())
                                .createdAt(r.getCreatedAt())
                                .updatedAt(r.getUpdatedAt())
                                .build())
                        .toList();
        return ResponseEntity.ok(list);
    }

    /** 業務ルールレビュー */
    @PostMapping("/{businessRuleId}/review")
    public ResponseEntity<ReviewDto.Response> review(
            @PathVariable String projectId,
            @PathVariable String businessRuleId,
            @RequestBody @Valid ReviewDto.ReviewRequest request) {
        ReviewDto.Response response =
                reviewBusinessRuleUseCase.execute(projectId, businessRuleId, request, "system");
        return ResponseEntity.ok(response);
    }
}

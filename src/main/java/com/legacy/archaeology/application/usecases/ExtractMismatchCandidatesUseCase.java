package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.domain.knowledge.BusinessRuleEntity;
import com.legacy.archaeology.domain.knowledge.BusinessRuleRepository;
import com.legacy.archaeology.domain.knowledge.ConfidenceLevel;
import com.legacy.archaeology.domain.knowledge.EvidenceEntity;
import com.legacy.archaeology.domain.knowledge.EvidenceRepository;
import com.legacy.archaeology.domain.knowledge.ReviewStatus;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.infrastructure.llm.LlmAdapter;
import com.legacy.archaeology.infrastructure.llm.LlmCandidateResponse;
import com.legacy.archaeology.infrastructure.prompt.PromptLoader;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 設計書と実装の不一致候補抽出ユースケース。
 * 文書解析済みのEvidenceと実装EvidenceをLLMへ投入し、不一致候補を生成する。
 *
 * AI利用ルール準拠:
 * - 出力は必ず構造化JSONで受け取る
 * - evidenceIds は文書側・実装側の両方を含める
 * - 候補は PENDING / INFERRED から開始する
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractMismatchCandidatesUseCase {

    private final LlmAdapter llmAdapter;
    private final PromptLoader promptLoader;
    private final BusinessRuleRepository businessRuleRepository;
    private final EvidenceRepository evidenceRepository;
    private final ProjectRepository projectRepository;
    private final IdGenerator idGenerator;
    private final AuditLogger auditLogger;

    private static final String MODEL_NAME = "gpt-4o";

    @Transactional
    public List<String> execute(String projectId, String jobId, String userId) {

        projectRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "プロジェクトが見つかりません: " + projectId));

        List<EvidenceEntity> docEvidences = evidenceRepository
                .findAllByProjectId(projectId).stream()
                .filter(e -> "DOCUMENT".equals(e.getEvidenceType()))
                .toList();

        List<EvidenceEntity> codeEvidences = evidenceRepository
                .findAllByProjectId(projectId).stream()
                .filter(e -> !"DOCUMENT".equals(e.getEvidenceType()))
                .toList();

        String userContext = buildMismatchContext(projectId, docEvidences, codeEvidences);
        String systemPrompt = promptLoader.load("mismatch-system");

        List<LlmCandidateResponse> candidates =
                llmAdapter.extractBusinessRuleCandidates(systemPrompt, userContext, MODEL_NAME);

        List<String> savedIds = new ArrayList<>();
        for (LlmCandidateResponse candidate : candidates) {
            BusinessRuleEntity rule = new BusinessRuleEntity();
            rule.setBusinessRuleId(idGenerator.generateBusinessRuleId());
            rule.setProjectId(projectId);
            rule.setRuleText(candidate.getText());
            rule.setConfidenceLevel(parseConfidenceLevel(candidate.getConfidenceLevel()));
            rule.setConfidenceScore(candidate.getConfidenceScore());
            rule.setReviewStatus(ReviewStatus.PENDING);
            rule.setEvidenceIds(candidate.getEvidenceIds());
            rule.setReason(candidate.getReason());
            rule.setModelName(candidate.getModelName());
            rule.setPromptVersion(candidate.getPromptVersion());
            businessRuleRepository.save(rule);
            savedIds.add(rule.getBusinessRuleId());
        }

        auditLogger.log(
                "MISMATCH_CANDIDATES_EXTRACTED",
                projectId, jobId, userId,
                "MismatchCandidate", null,
                "候補数=" + savedIds.size() + " model=" + MODEL_NAME);

        log.info("不一致候補抽出完了 projectId={} count={}", projectId, savedIds.size());
        return savedIds;
    }

    private String buildMismatchContext(
            String projectId,
            List<EvidenceEntity> docEvidences,
            List<EvidenceEntity> codeEvidences) {

        StringBuilder sb = new StringBuilder();
        sb.append("## プロジェクト: ").append(projectId).append("\n\n");

        sb.append("## 設計書記述\n");
        for (EvidenceEntity ev : docEvidences) {
            sb.append("### ").append(ev.getEvidenceId()).append("\n");
            if (ev.getSnippet() != null) {
                sb.append(ev.getSnippet()).append("\n\n");
            }
        }

        sb.append("## 実装コード\n");
        for (EvidenceEntity ev : codeEvidences) {
            sb.append("### ").append(ev.getEvidenceId()).append("\n");
            if (ev.getSnippet() != null) {
                sb.append("```\n").append(ev.getSnippet()).append("\n```\n\n");
            }
        }
        return sb.toString();
    }

    private ConfidenceLevel parseConfidenceLevel(String level) {
        try {
            return ConfidenceLevel.valueOf(level.toUpperCase());
        } catch (Exception e) {
            return ConfidenceLevel.INFERRED;
        }
    }
}

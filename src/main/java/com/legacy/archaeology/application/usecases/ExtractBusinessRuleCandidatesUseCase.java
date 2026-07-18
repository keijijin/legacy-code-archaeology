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
 * 業務ルール候補抽出ユースケース。
 * 解析済みコード・IR・Evidenceを文脈化し、LLMへ投入して候補を生成する。
 *
 * AI利用ルール準拠:
 * - 出力は必ず構造化JSONで受け取る
 * - evidenceIds が空の候補は破棄する
 * - すべての候補は PENDING から開始する
 * - INFERRED → CONFIRMED の自動遷移は禁止
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractBusinessRuleCandidatesUseCase {

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

        // 既存のEvidenceをコンテキスト化
        List<EvidenceEntity> evidences = evidenceRepository.findAllByProjectId(projectId);
        String userContext = buildContext(projectId, evidences);

        // プロンプト読み込み（版管理済み）
        String systemPrompt = promptLoader.load("business-rule-system");

        // LLM呼び出し
        List<LlmCandidateResponse> candidates =
                llmAdapter.extractBusinessRuleCandidates(systemPrompt, userContext, MODEL_NAME);

        // 候補保存
        List<String> savedRuleIds = new ArrayList<>();
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
            savedRuleIds.add(rule.getBusinessRuleId());
        }

        auditLogger.log(
                "BUSINESS_RULE_CANDIDATES_EXTRACTED",
                projectId, jobId, userId,
                "BusinessRule", null,
                "候補数=" + savedRuleIds.size() + " model=" + MODEL_NAME
                        + " promptVersion=" + promptLoader.getPromptVersion());

        log.info("業務ルール候補抽出完了 projectId={} count={}", projectId, savedRuleIds.size());
        return savedRuleIds;
    }

    private String buildContext(String projectId, List<EvidenceEntity> evidences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## プロジェクト: ").append(projectId).append("\n\n");
        sb.append("## 解析済みコード断片\n");
        for (EvidenceEntity ev : evidences) {
            sb.append("### ").append(ev.getEvidenceId())
                    .append(" [").append(ev.getEvidenceType()).append("]\n");
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

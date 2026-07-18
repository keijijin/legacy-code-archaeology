package com.legacy.archaeology.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LLMからの構造化レスポンス。
 * AI出力は必ず構造化JSONで受け取る。自由文のまま保存してはならない。
 * evidenceIds・confidenceLevel 未設定は不正レスポンスとして扱う。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmCandidateResponse {

    /** 候補種別: BusinessRule / BusinessFunction / MismatchCandidate */
    private String candidateType;

    /** 候補本文 */
    private String text;

    /** 信頼度状態: CONFIRMED / LIKELY / INFERRED / CONFLICTED / UNKNOWN */
    private String confidenceLevel;

    /** 信頼度スコア: 0.0〜1.0 */
    private Double confidenceScore;

    /** 根拠証拠IDリスト（必須。空は不正） */
    private List<String> evidenceIds;

    /** 判定理由 */
    private String reason;

    /** レビュー状態（初期値: Pending） */
    private String reviewStatus = "Pending";

    /** 使用モデル名 */
    private String modelName;

    /** プロンプト版 */
    private String promptVersion;

    /** バリデーション: evidenceIds と confidenceLevel は必須 */
    public boolean isValid() {
        return confidenceLevel != null
                && !confidenceLevel.isBlank()
                && evidenceIds != null
                && !evidenceIds.isEmpty()
                && text != null
                && !text.isBlank();
    }
}

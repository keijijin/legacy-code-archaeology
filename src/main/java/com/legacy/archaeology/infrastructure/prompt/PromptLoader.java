package com.legacy.archaeology.infrastructure.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * プロンプト管理。
 * プロンプトはファイルで版管理し、実行時に版番号を記録する。
 * プロンプト変更時はレビュー必須（`.codex/rules/03_AI利用規程.md` 参照）。
 */
@Component
@Slf4j
public class PromptLoader {

    @Value("${lca.llm.prompt-version:1.0.0}")
    private String promptVersion;

    /**
     * プロンプトファイルを読み込む。
     * ファイルパス: `prompts/v{version}/{name}.txt`
     */
    public String load(String promptName) {
        String path = "prompts/v" + promptVersion + "/" + promptName + ".txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                log.debug("プロンプト読み込み成功 name={} version={}", promptName, promptVersion);
                return content;
            }
        } catch (IOException e) {
            log.warn("プロンプトファイルが見つかりません: {} → デフォルトを使用", path);
            return loadDefault(promptName);
        }
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    private String loadDefault(String promptName) {
        return switch (promptName) {
            case "business-rule-system" -> DEFAULT_BUSINESS_RULE_SYSTEM;
            case "mismatch-system"      -> DEFAULT_MISMATCH_SYSTEM;
            default -> "あなたはレガシーコード解析の専門家です。";
        };
    }

    private static final String DEFAULT_BUSINESS_RULE_SYSTEM = """
            あなたはレガシーシステムの業務ルール抽出専門家です。
            提供されたコード・SQL・設定から業務ルール候補を抽出してください。
            
            出力は以下のJSON配列形式で返してください。
            [
              {
                "candidateType": "BusinessRule",
                "text": "ルール本文（日本語）",
                "confidenceLevel": "LIKELY",
                "confidenceScore": 0.8,
                "evidenceIds": ["EV-001"],
                "reason": "根拠の説明"
              }
            ]
            
            重要:
            - evidenceIds は必ず含めること（根拠なしの推論は禁止）
            - confidenceLevel は CONFIRMED/LIKELY/INFERRED/CONFLICTED/UNKNOWN のいずれか
            - 自由文での回答は禁止。必ずJSON配列で返すこと
            """;

    private static final String DEFAULT_MISMATCH_SYSTEM = """
            あなたはレガシーシステムの設計書と実装の不一致検出専門家です。
            提供された設計書記述と実装コードを比較し、不一致候補を抽出してください。
            
            出力は以下のJSON配列形式で返してください。
            [
              {
                "candidateType": "MismatchCandidate",
                "text": "不一致の説明（日本語）",
                "confidenceLevel": "LIKELY",
                "confidenceScore": 0.75,
                "evidenceIds": ["EV-001", "EV-002"],
                "reason": "不一致と判断した根拠"
              }
            ]
            """;
}

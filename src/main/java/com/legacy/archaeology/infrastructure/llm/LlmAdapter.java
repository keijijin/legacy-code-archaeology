package com.legacy.archaeology.infrastructure.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * LLM呼び出しアダプタ。
 * Spring AI 経由でLLMを呼び出し、構造化JSONを受け取る。
 * 外部送信可否を設定で制御する。
 * AI利用ルール: 出力に evidenceIds と confidenceLevel がない場合は不正として破棄する。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmAdapter {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${lca.llm.external-send-enabled:false}")
    private boolean externalSendEnabled;

    @Value("${lca.llm.prompt-version:1.0.0}")
    private String promptVersion;

    /**
     * 業務ルール候補を抽出する。
     * 外部送信が無効の場合はスタブ結果を返す。
     */
    public List<LlmCandidateResponse> extractBusinessRuleCandidates(
            String systemPrompt, String userContext, String modelName) {

        if (!externalSendEnabled) {
            log.warn("LLM外部送信が無効です。スタブ結果を返します。");
            return stubBusinessRuleCandidates(modelName);
        }

        log.info("LLM呼び出し開始 type=BusinessRule model={} promptVersion={}", modelName, promptVersion);

        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userContext)));

            String rawResponse = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.debug("LLMレスポンス受信 length={}", rawResponse.length());

            List<LlmCandidateResponse> candidates = parseResponse(rawResponse, modelName);
            List<LlmCandidateResponse> valid = candidates.stream()
                    .filter(c -> {
                        if (!c.isValid()) {
                            log.warn("不正なLLMレスポンス: evidenceIds または confidenceLevel が未設定 text={}",
                                    c.getText());
                            return false;
                        }
                        return true;
                    })
                    .peek(c -> c.setPromptVersion(promptVersion))
                    .peek(c -> c.setModelName(modelName))
                    .toList();

            log.info("LLM呼び出し完了 total={} valid={}", candidates.size(), valid.size());
            return valid;

        } catch (Exception e) {
            log.error("LLM呼び出しエラー", e);
            throw new LlmInvocationException("LLM呼び出しに失敗しました", e);
        }
    }

    private List<LlmCandidateResponse> parseResponse(String rawResponse, String modelName) {
        try {
            String json = extractJson(rawResponse);
            return objectMapper.readValue(json, new TypeReference<List<LlmCandidateResponse>>() {});
        } catch (Exception e) {
            log.warn("LLMレスポンスのJSON解析失敗: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return "[]";
    }

    private List<LlmCandidateResponse> stubBusinessRuleCandidates(String modelName) {
        LlmCandidateResponse stub = new LlmCandidateResponse();
        stub.setCandidateType("BusinessRule");
        stub.setText("【スタブ】外部LLM送信が無効のため、実際の候補は生成されていません");
        stub.setConfidenceLevel("INFERRED");
        stub.setConfidenceScore(0.0);
        stub.setEvidenceIds(List.of("EV-STUB-001"));
        stub.setReason("外部送信無効のスタブ結果です。lca.llm.external-send-enabled=true にして再実行してください");
        stub.setReviewStatus("Pending");
        stub.setModelName(modelName);
        stub.setPromptVersion(promptVersion);
        return List.of(stub);
    }
}

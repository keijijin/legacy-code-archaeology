package com.legacy.archaeology.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LlmCandidateResponseTest {

    @Test
    void evidenceIdsとconfidenceLevelが揃っている場合はvalidであること() {
        LlmCandidateResponse response = new LlmCandidateResponse();
        response.setCandidateType("BusinessRule");
        response.setText("顧客区分が法人の場合に口座開設可能");
        response.setConfidenceLevel("LIKELY");
        response.setConfidenceScore(0.82);
        response.setEvidenceIds(List.of("EV-001", "EV-002"));
        response.setReason("条件分岐と設計書の記述が一致");

        assertThat(response.isValid()).isTrue();
    }

    @Test
    void evidenceIdsが空の場合はinvalidであること() {
        LlmCandidateResponse response = new LlmCandidateResponse();
        response.setCandidateType("BusinessRule");
        response.setText("何らかのルール");
        response.setConfidenceLevel("INFERRED");
        response.setEvidenceIds(List.of()); // 空

        assertThat(response.isValid()).isFalse();
    }

    @Test
    void confidenceLevelが未設定の場合はinvalidであること() {
        LlmCandidateResponse response = new LlmCandidateResponse();
        response.setCandidateType("BusinessRule");
        response.setText("何らかのルール");
        response.setEvidenceIds(List.of("EV-001"));
        // confidenceLevel 未設定

        assertThat(response.isValid()).isFalse();
    }

    @Test
    void textが未設定の場合はinvalidであること() {
        LlmCandidateResponse response = new LlmCandidateResponse();
        response.setCandidateType("BusinessRule");
        response.setConfidenceLevel("LIKELY");
        response.setEvidenceIds(List.of("EV-001"));
        // text 未設定

        assertThat(response.isValid()).isFalse();
    }
}

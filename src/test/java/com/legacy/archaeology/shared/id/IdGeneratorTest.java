package com.legacy.archaeology.shared.id;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdGeneratorTest {

    private final IdGenerator idGenerator = new IdGenerator();

    @Test
    void プロジェクトIDはPRJプレフィックスを持つこと() {
        String id = idGenerator.generateProjectId();
        assertThat(id).startsWith("PRJ-");
        assertThat(id).hasSize(12);
    }

    @Test
    void ジョブIDはJOBプレフィックスを持つこと() {
        String id = idGenerator.generateJobId();
        assertThat(id).startsWith("JOB-");
    }

    @Test
    void 連続生成で重複しないこと() {
        String id1 = idGenerator.generateProjectId();
        String id2 = idGenerator.generateProjectId();
        assertThat(id1).isNotEqualTo(id2);
    }
}

package com.legacy.archaeology.infrastructure.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {PromptLoader.class})
@ActiveProfiles("test")
class PromptLoaderTest {

    @Autowired
    private PromptLoader promptLoader;

    @Test
    void デフォルトプロンプトが取得できること() {
        String prompt = promptLoader.load("business-rule-system");
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("JSON");
    }

    @Test
    void 不一致検出プロンプトが取得できること() {
        String prompt = promptLoader.load("mismatch-system");
        assertThat(prompt).isNotBlank();
    }
}

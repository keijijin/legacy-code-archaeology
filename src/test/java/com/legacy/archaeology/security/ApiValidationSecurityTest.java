package com.legacy.archaeology.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.legacy.archaeology.presentation.api.GlobalExceptionHandler;
import com.legacy.archaeology.presentation.api.ProjectController;
import com.legacy.archaeology.application.usecases.CreateProjectUseCase;
import com.legacy.archaeology.application.usecases.IngestAssetUseCase;
import com.legacy.archaeology.application.usecases.SubmitAnalysisJobUseCase;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * セキュリティ観点のAPI検証テスト。
 * - 入力バリデーション
 * - エラーレスポンスに内部詳細を出しすぎないこと
 */
@ExtendWith(MockitoExtension.class)
class ApiValidationSecurityTest {

    @Mock private CreateProjectUseCase createProjectUseCase;
    @Mock private IngestAssetUseCase ingestAssetUseCase;
    @Mock private SubmitAnalysisJobUseCase submitAnalysisJobUseCase;
    @Mock private ProjectRepository projectRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProjectController controller =
                new ProjectController(
                        createProjectUseCase,
                        ingestAssetUseCase,
                        submitAnalysisJobUseCase,
                        projectRepository);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void プロジェクト名未指定は400になり秘密情報を含まないこと() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/projects")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"description\":\"x\"}"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("VALIDATION_ERROR");
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("token");
        assertThat(body).doesNotContain("secret");
    }

    @Test
    void 不正JSONでも500詳細スタックをクライアントへ返さないこと() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/projects")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{"))
                        .andExpect(status().is4xxClientError())
                        .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("at com.legacy");
    }
}

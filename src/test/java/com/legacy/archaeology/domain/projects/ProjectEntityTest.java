package com.legacy.archaeology.domain.projects;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProjectEntityTest {

    @Test
    void プロジェクトエンティティのデフォルトステータスはACTIVEであること() {
        ProjectEntity project = new ProjectEntity();
        project.setProjectId("PRJ-TEST001");
        project.setName("テストプロジェクト");
        project.onCreate();

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(project.getCreatedAt()).isNotNull();
        assertThat(project.getUpdatedAt()).isNotNull();
    }
}

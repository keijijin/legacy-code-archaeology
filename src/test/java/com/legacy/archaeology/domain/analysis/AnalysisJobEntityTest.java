package com.legacy.archaeology.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnalysisJobEntityTest {

    @Test
    void ジョブのデフォルトステータスはQUEUEDであること() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setJobId("JOB-TEST001");
        job.setProjectId("PRJ-TEST001");
        job.setJobType(JobType.STATIC_ANALYSIS);
        job.onCreate();

        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(job.getCreatedAt()).isNotNull();
    }

    @Test
    void 終了状態の判定が正しいこと() {
        AnalysisJobEntity job = new AnalysisJobEntity();

        job.setStatus(JobStatus.QUEUED);
        assertThat(job.isTerminated()).isFalse();

        job.setStatus(JobStatus.RUNNING);
        assertThat(job.isTerminated()).isFalse();

        job.setStatus(JobStatus.SUCCEEDED);
        assertThat(job.isTerminated()).isTrue();

        job.setStatus(JobStatus.FAILED);
        assertThat(job.isTerminated()).isTrue();

        job.setStatus(JobStatus.PARTIAL);
        assertThat(job.isTerminated()).isTrue();
    }
}

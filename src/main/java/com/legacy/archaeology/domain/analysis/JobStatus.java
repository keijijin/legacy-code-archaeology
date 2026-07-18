package com.legacy.archaeology.domain.analysis;

/** ジョブ実行状態 */
public enum JobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    PARTIAL
}

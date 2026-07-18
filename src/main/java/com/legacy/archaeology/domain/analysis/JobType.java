package com.legacy.archaeology.domain.analysis;

/** ジョブ種別 */
public enum JobType {
    INGESTION,
    STATIC_ANALYSIS,
    DOCUMENT_ANALYSIS,
    LOG_ANALYSIS,
    AI_EXTRACTION,
    GRAPH_SYNC,
    REPORT_EXPORT,
    REANALYSIS
}

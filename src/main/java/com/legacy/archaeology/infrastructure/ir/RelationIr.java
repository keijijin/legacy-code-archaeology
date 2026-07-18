package com.legacy.archaeology.infrastructure.ir;

import lombok.Builder;
import lombok.Value;

/**
 * 中間表現: 関係（エッジ）。
 * CALLS / READS / WRITES / USES / IMPLEMENTS 等を保持する。
 */
@Value
@Builder
public class RelationIr {

    String id;              // REL-xxxx
    String projectId;
    String relationType;    // CALLS / READS / WRITES / USES / IMPLEMENTS 等
    String fromNodeId;
    String toNodeId;
    String sourceAssetId;
    String sourcePath;
    int startLine;
    int endLine;
}

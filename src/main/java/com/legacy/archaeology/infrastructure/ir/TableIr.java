package com.legacy.archaeology.infrastructure.ir;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 中間表現: DBテーブル。
 * DDLから抽出したテーブル・カラム定義を保持する。
 */
@Value
@Builder
public class TableIr {

    String id;              // ENT-xxxx
    String projectId;
    String sourceAssetId;
    String sourcePath;
    String tableName;
    List<ColumnIr> columns;
    String parserVersion;

    @Value
    @Builder
    public static class ColumnIr {
        String name;
        String dataType;
    }
}

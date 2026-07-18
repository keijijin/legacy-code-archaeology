package com.legacy.archaeology.infrastructure.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 知識グラフノード: DBテーブル。 */
@Node("Table")
@Getter
@Setter
@NoArgsConstructor
public class TableNode {

    @Id
    private String id;

    @Property("projectId")
    private String projectId;

    @Property("sourceAssetId")
    private String sourceAssetId;

    @Property("sourcePath")
    private String sourcePath;

    @Property("tableName")
    private String tableName;

    @Property("parserVersion")
    private String parserVersion;
}

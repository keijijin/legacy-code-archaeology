package com.legacy.archaeology.infrastructure.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 知識グラフノード: プログラム（クラス）。 */
@Node("Program")
@Getter
@Setter
@NoArgsConstructor
public class ProgramNode {

    @Id
    private String id;

    @Property("projectId")
    private String projectId;

    @Property("sourceAssetId")
    private String sourceAssetId;

    @Property("sourcePath")
    private String sourcePath;

    @Property("language")
    private String language;

    @Property("packageName")
    private String packageName;

    @Property("className")
    private String className;

    @Property("isInterface")
    private boolean isInterface;

    @Property("lineNumber")
    private int lineNumber;

    @Property("parserVersion")
    private String parserVersion;
}

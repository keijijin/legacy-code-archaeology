package com.legacy.archaeology.infrastructure.graph;

import java.util.List;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 知識グラフノード: Camelルート。 */
@Node("Route")
@Getter
@Setter
@NoArgsConstructor
public class RouteNode {

    @Id
    private String id;

    @Property("projectId")
    private String projectId;

    @Property("sourceAssetId")
    private String sourceAssetId;

    @Property("sourcePath")
    private String sourcePath;

    @Property("routeId")
    private String routeId;

    @Property("fromUri")
    private String fromUri;

    @Property("steps")
    private List<String> steps;

    @Property("parserVersion")
    private String parserVersion;
}

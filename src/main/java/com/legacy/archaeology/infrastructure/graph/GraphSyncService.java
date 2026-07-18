package com.legacy.archaeology.infrastructure.graph;

import com.legacy.archaeology.infrastructure.ir.ProgramIr;
import com.legacy.archaeology.infrastructure.ir.RouteIr;
import com.legacy.archaeology.infrastructure.ir.TableIr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 中間表現（IR）を知識グラフ（Neo4j）へ反映するサービス。
 * 差分反映：同一IDノードはMERGEで上書き更新する。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraphSyncService {

    private final Neo4jClient neo4jClient;

    public void syncPrograms(List<ProgramIr> programs) {
        for (ProgramIr ir : programs) {
            neo4jClient.query("""
                    MERGE (p:Program {id: $id})
                    SET p.projectId       = $projectId,
                        p.sourceAssetId   = $sourceAssetId,
                        p.sourcePath      = $sourcePath,
                        p.language        = $language,
                        p.packageName     = $packageName,
                        p.className       = $className,
                        p.isInterface     = $isInterface,
                        p.lineNumber      = $lineNumber,
                        p.parserVersion   = $parserVersion
                    """)
                    .bindAll(Map.of(
                            "id", ir.getId(),
                            "projectId", ir.getProjectId(),
                            "sourceAssetId", ir.getSourceAssetId() == null ? "" : ir.getSourceAssetId(),
                            "sourcePath", ir.getSourcePath(),
                            "language", ir.getLanguage(),
                            "packageName", ir.getPackageName() == null ? "" : ir.getPackageName(),
                            "className", ir.getClassName(),
                            "isInterface", ir.isInterface(),
                            "lineNumber", ir.getLineNumber(),
                            "parserVersion", ir.getParserVersion()))
                    .run();
        }
        log.info("知識グラフ反映完了 Programs count={}", programs.size());
    }

    public void syncRoutes(List<RouteIr> routes) {
        for (RouteIr ir : routes) {
            neo4jClient.query("""
                    MERGE (r:Route {id: $id})
                    SET r.projectId     = $projectId,
                        r.sourceAssetId = $sourceAssetId,
                        r.sourcePath    = $sourcePath,
                        r.routeId       = $routeId,
                        r.fromUri       = $fromUri,
                        r.parserVersion = $parserVersion
                    """)
                    .bindAll(Map.of(
                            "id", ir.getId(),
                            "projectId", ir.getProjectId(),
                            "sourceAssetId", ir.getSourceAssetId() == null ? "" : ir.getSourceAssetId(),
                            "sourcePath", ir.getSourcePath(),
                            "routeId", ir.getRouteId(),
                            "fromUri", ir.getFromUri() == null ? "" : ir.getFromUri(),
                            "parserVersion", ir.getParserVersion()))
                    .run();
        }
        log.info("知識グラフ反映完了 Routes count={}", routes.size());
    }

    public void syncTables(List<TableIr> tables) {
        for (TableIr ir : tables) {
            neo4jClient.query("""
                    MERGE (t:Table {id: $id})
                    SET t.projectId     = $projectId,
                        t.sourceAssetId = $sourceAssetId,
                        t.sourcePath    = $sourcePath,
                        t.tableName     = $tableName,
                        t.parserVersion = $parserVersion
                    """)
                    .bindAll(Map.of(
                            "id", ir.getId(),
                            "projectId", ir.getProjectId(),
                            "sourceAssetId", ir.getSourceAssetId() == null ? "" : ir.getSourceAssetId(),
                            "sourcePath", ir.getSourcePath(),
                            "tableName", ir.getTableName(),
                            "parserVersion", ir.getParserVersion()))
                    .run();

            for (TableIr.ColumnIr col : ir.getColumns()) {
                neo4jClient.query("""
                        MERGE (c:Column {id: $id})
                        SET c.name     = $name,
                            c.dataType = $dataType,
                            c.tableId  = $tableId
                        WITH c
                        MATCH (t:Table {id: $tableId})
                        MERGE (t)-[:HAS_COLUMN]->(c)
                        """)
                        .bindAll(Map.of(
                                "id", ir.getId() + "-" + col.getName(),
                                "name", col.getName(),
                                "dataType", col.getDataType() == null ? "" : col.getDataType(),
                                "tableId", ir.getId()))
                        .run();
            }
        }
        log.info("知識グラフ反映完了 Tables count={}", tables.size());
    }

    public void createRelation(String fromId, String toId, String relationType, String projectId) {
        neo4jClient.query("""
                MATCH (a {id: $fromId}), (b {id: $toId})
                MERGE (a)-[r:RELATION {type: $relationType, projectId: $projectId}]->(b)
                """)
                .bindAll(Map.of(
                        "fromId", fromId,
                        "toId", toId,
                        "relationType", relationType,
                        "projectId", projectId))
                .run();
        log.debug("関係作成 {} -[{}]-> {}", fromId, relationType, toId);
    }
}

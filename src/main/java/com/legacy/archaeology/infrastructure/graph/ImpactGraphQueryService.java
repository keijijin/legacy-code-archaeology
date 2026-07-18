package com.legacy.archaeology.infrastructure.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

/**
 * 影響分析向け Neo4j クエリサービス。
 * Evidence First: 返却結果には sourcePath / relationPath / depth を含める。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImpactGraphQueryService {

    private final Neo4jClient neo4jClient;

    public Optional<Map<String, Object>> findStartNode(
            String projectId, String targetType, String targetId, String targetName) {

        String type = normalizeType(targetType);

        if (targetId != null && !targetId.isBlank()) {
            Collection<Map<String, Object>> byId =
                    neo4jClient
                            .query(
                                    """
                                    MATCH (n)
                                    WHERE n.projectId = $projectId AND n.id = $targetId
                                    RETURN labels(n)[0] AS nodeType,
                                           n.id AS nodeId,
                                           coalesce(n.className, n.routeId, n.tableName, n.name, n.id) AS name,
                                           coalesce(n.sourcePath, '') AS sourcePath
                                    LIMIT 1
                                    """)
                            .bind(projectId)
                            .to("projectId")
                            .bind(targetId)
                            .to("targetId")
                            .fetch()
                            .all();
            if (!byId.isEmpty()) {
                return Optional.of(byId.iterator().next());
            }
        }

        if (targetName == null || targetName.isBlank()) {
            return Optional.empty();
        }

        String query =
                switch (type) {
                    case "COLUMN" ->
                            """
                            MATCH (n:Column)
                            WHERE n.projectId = $projectId AND toLower(n.name) = toLower($targetName)
                            RETURN 'Column' AS nodeType, n.id AS nodeId, n.name AS name,
                                   coalesce(n.sourcePath, '') AS sourcePath
                            LIMIT 1
                            """;
                    case "TABLE" ->
                            """
                            MATCH (n:Table)
                            WHERE n.projectId = $projectId AND toLower(n.tableName) = toLower($targetName)
                            RETURN 'Table' AS nodeType, n.id AS nodeId, n.tableName AS name,
                                   coalesce(n.sourcePath, '') AS sourcePath
                            LIMIT 1
                            """;
                    case "ROUTE" ->
                            """
                            MATCH (n:Route)
                            WHERE n.projectId = $projectId AND toLower(n.routeId) = toLower($targetName)
                            RETURN 'Route' AS nodeType, n.id AS nodeId, n.routeId AS name,
                                   coalesce(n.sourcePath, '') AS sourcePath
                            LIMIT 1
                            """;
                    case "ENDPOINT", "API" ->
                            """
                            MATCH (n)
                            WHERE n.projectId = $projectId
                              AND (
                                (n:Endpoint AND toLower(coalesce(n.name, n.uri, '')) CONTAINS toLower($targetName))
                                OR (n:Route AND toLower(coalesce(n.fromUri, '')) CONTAINS toLower($targetName))
                              )
                            RETURN labels(n)[0] AS nodeType, n.id AS nodeId,
                                   coalesce(n.name, n.uri, n.fromUri, n.routeId, n.id) AS name,
                                   coalesce(n.sourcePath, '') AS sourcePath
                            LIMIT 1
                            """;
                    default ->
                            """
                            MATCH (n:Program)
                            WHERE n.projectId = $projectId AND toLower(n.className) = toLower($targetName)
                            RETURN 'Program' AS nodeType, n.id AS nodeId, n.className AS name,
                                   coalesce(n.sourcePath, '') AS sourcePath
                            LIMIT 1
                            """;
                };

        Collection<Map<String, Object>> byName =
                neo4jClient
                        .query(query)
                        .bind(projectId)
                        .to("projectId")
                        .bind(targetName)
                        .to("targetName")
                        .fetch()
                        .all();
        return byName.isEmpty() ? Optional.empty() : Optional.of(byName.iterator().next());
    }

    public List<Map<String, Object>> findImpactedNodes(
            String projectId, String startNodeId, int maxDepth) {

        int depth = Math.max(1, Math.min(maxDepth, 5));

        Collection<Map<String, Object>> rows =
                neo4jClient
                        .query(
                                """
                                MATCH (start {id: $startNodeId})
                                WHERE start.projectId = $projectId
                                MATCH path = (start)-[*1.."""
                                        + depth
                                        + """
                                ]-(affected)
                                WHERE affected.projectId = $projectId AND affected.id <> start.id
                                WITH affected, path, length(path) AS depth
                                ORDER BY depth ASC
                                WITH affected, collect(path)[0] AS path, min(depth) AS depth
                                RETURN DISTINCT
                                  labels(affected)[0] AS nodeType,
                                  affected.id AS nodeId,
                                  coalesce(
                                    affected.className,
                                    affected.routeId,
                                    affected.tableName,
                                    affected.name,
                                    affected.uri,
                                    affected.id
                                  ) AS name,
                                  depth AS depth,
                                  [r IN relationships(path) |
                                    type(r) + CASE WHEN r.type IS NULL THEN '' ELSE ':' + r.type END
                                  ] AS relationPath,
                                  coalesce(affected.sourcePath, '') AS sourcePath
                                ORDER BY depth, nodeType, name
                                LIMIT 200
                                """)
                        .bind(projectId)
                        .to("projectId")
                        .bind(startNodeId)
                        .to("startNodeId")
                        .fetch()
                        .all();

        return new ArrayList<>(rows);
    }

    public List<Map<String, Object>> findRelatedTests(String projectId, String startNodeId) {
        Collection<Map<String, Object>> rows =
                neo4jClient
                        .query(
                                """
                                MATCH (start {id: $startNodeId})
                                WHERE start.projectId = $projectId
                                MATCH (start)-[*0..3]-(n)
                                WHERE n.projectId = $projectId
                                MATCH (t:TestCase)-[rel]->(n)
                                WHERE type(rel) = 'RELATION' AND coalesce(rel.type, '') = 'VERIFIED_BY'
                                   OR type(rel) = 'VERIFIED_BY'
                                RETURN DISTINCT
                                  t.id AS testId,
                                  coalesce(t.name, t.id) AS testName,
                                  coalesce(t.sourcePath, '') AS sourcePath,
                                  n.id AS verifiedTargetId,
                                  coalesce(n.className, n.routeId, n.tableName, n.name, n.id) AS verifiedTargetName
                                LIMIT 100
                                """)
                        .bind(projectId)
                        .to("projectId")
                        .bind(startNodeId)
                        .to("startNodeId")
                        .fetch()
                        .all();
        return new ArrayList<>(rows);
    }

    public List<Map<String, Object>> findMigrationSignals(String projectId) {
        Collection<Map<String, Object>> rows =
                neo4jClient
                        .query(
                                """
                                MATCH (n)
                                WHERE n.projectId = $projectId
                                WITH n,
                                     toLower(coalesce(n.fromUri, '')) AS fromUri,
                                     toLower(coalesce(n.sourcePath, '')) AS sourcePath,
                                     toLower(coalesce(n.className, n.routeId, n.name, '')) AS label
                                WHERE fromUri CONTAINS 'localhost'
                                   OR fromUri CONTAINS '127.0.0.1'
                                   OR fromUri CONTAINS 'file:'
                                   OR fromUri CONTAINS 'jdbc:oracle'
                                   OR fromUri STARTS WITH 'jms:'
                                   OR fromUri CONTAINS 'soap'
                                   OR sourcePath CONTAINS 'c:\\\\'
                                   OR sourcePath CONTAINS '/var/local'
                                RETURN labels(n)[0] AS nodeType,
                                       n.id AS nodeId,
                                       coalesce(n.className, n.routeId, n.tableName, n.name, n.id) AS name,
                                       coalesce(n.fromUri, '') AS fromUri,
                                       coalesce(n.sourcePath, '') AS sourcePath
                                LIMIT 100
                                """)
                        .bind(projectId)
                        .to("projectId")
                        .fetch()
                        .all();
        return new ArrayList<>(rows);
    }

    public List<Map<String, Object>> findModernizationTargets(String projectId) {
        Collection<Map<String, Object>> rows =
                neo4jClient
                        .query(
                                """
                                MATCH (n)
                                WHERE n.projectId = $projectId
                                  AND (n:Program OR n:Route OR n:Table OR n:Endpoint)
                                OPTIONAL MATCH (n)-[r]-(m)
                                WHERE m.projectId = $projectId
                                WITH n, count(DISTINCT m) AS degree
                                RETURN labels(n)[0] AS nodeType,
                                       n.id AS nodeId,
                                       coalesce(n.className, n.routeId, n.tableName, n.name, n.uri, n.id) AS name,
                                       coalesce(n.sourcePath, '') AS sourcePath,
                                       coalesce(n.fromUri, '') AS fromUri,
                                       degree AS degree
                                ORDER BY degree DESC, name
                                LIMIT 100
                                """)
                        .bind(projectId)
                        .to("projectId")
                        .fetch()
                        .all();
        return new ArrayList<>(rows);
    }

    public String resolveImpactLevel(int depth, List<String> relationPath) {
        String joined =
                relationPath == null
                        ? ""
                        : String.join(" ", relationPath).toUpperCase(Locale.ROOT);
        if (depth <= 1 && (joined.contains("WRITE") || joined.contains("HAS_COLUMN"))) {
            return "HIGH";
        }
        if (depth <= 2) {
            return "MEDIUM";
        }
        if (depth >= 3) {
            return "LOW";
        }
        return "UNKNOWN";
    }

    public Map<String, Object> toSafeMap(Map<String, Object> raw) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (raw == null) {
            return safe;
        }
        raw.forEach((k, v) -> safe.put(k, v));
        return safe;
    }

    private String normalizeType(String targetType) {
        if (targetType == null) {
            return "PROGRAM";
        }
        return targetType.trim().toUpperCase(Locale.ROOT);
    }
}

package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.GraphQueryDto;
import com.legacy.archaeology.infrastructure.graph.GraphQueryExecutor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

/**
 * Neo4j 直接クエリ UseCase。
 * Evidence First: 返却するグラフデータには常にノード ID・ラベル・ソースパスを含める。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecuteGraphQueryUseCase {

    private final Neo4jClient neo4jClient;
    private final GraphQueryExecutor graphQueryExecutor;

    public GraphQueryDto.Response execute(String projectId, GraphQueryDto.Request request) {
        log.info("[GraphQuery] projectId={} mode={} cypher={}",
                projectId, request.getMode(), request.getCypher().replaceAll("\\s+", " ").trim());

        String mode = request.getMode() == null ? "GRAPH" : request.getMode().toUpperCase();

        // Cypher 実行（projectId を自動バインド）
        List<Map<String, Object>> rows = executeQuery(projectId, request);

        return switch (mode) {
            case "TABLE" -> buildTableResponse(projectId, rows);
            case "MERMAID" -> buildMermaidResponse(projectId, rows);
            case "SEQUENCE" -> buildSequenceResponse(projectId, rows);
            default -> buildGraphResponse(projectId, rows);
        };
    }

    private List<Map<String, Object>> executeQuery(String projectId, GraphQueryDto.Request request) {
        try {
            Collection<Map<String, Object>> raw;
            if (request.isBindProjectId()) {
                raw = neo4jClient.query(request.getCypher())
                        .bind(projectId).to("projectId")
                        .fetch().all();
            } else {
                raw = neo4jClient.query(request.getCypher())
                        .fetch().all();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : raw) {
                result.add(toSafeMap(row));
            }
            return result;
        } catch (Exception e) {
            log.warn("[GraphQuery] Cypher 実行エラー: {}", e.getMessage());
            throw new IllegalArgumentException("Cypher 実行エラー: " + e.getMessage(), e);
        }
    }

    /** GRAPH モード: rows から nodes / edges を自動抽出 */
    private GraphQueryDto.Response buildGraphResponse(String projectId, List<Map<String, Object>> rows) {
        List<GraphQueryDto.GraphNode> nodes = new ArrayList<>();
        List<GraphQueryDto.GraphEdge> edges = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            // ノード列（nodeId / sourceId / targetId がある場合）
            extractNode(row, "nodeId", "nodeLabel", "nodeName", "nodeSourcePath", nodes);
            extractNode(row, "sourceId", "sourceLabel", "sourceName", "sourceSourcePath", nodes);
            extractNode(row, "targetId", "targetLabel", "targetName", "targetSourcePath", nodes);

            // エッジ列（sourceId + targetId + relType）
            String srcId = strVal(row, "sourceId");
            String tgtId = strVal(row, "targetId");
            String relType = strVal(row, "relType");
            if (srcId != null && tgtId != null) {
                edges.add(GraphQueryDto.GraphEdge.builder()
                        .source(srcId)
                        .target(tgtId)
                        .type(relType != null ? relType : "RELATED")
                        .build());
            }
        }

        // deduplicate nodes by id
        Map<String, GraphQueryDto.GraphNode> nodeMap = new LinkedHashMap<>();
        for (GraphQueryDto.GraphNode n : nodes) {
            nodeMap.putIfAbsent(n.getId(), n);
        }

        return GraphQueryDto.Response.builder()
                .projectId(projectId)
                .mode("GRAPH")
                .rowCount(rows.size())
                .rows(rows)
                .nodes(new ArrayList<>(nodeMap.values()))
                .edges(edges)
                .build();
    }

    /** TABLE モード: rows をそのまま返す */
    private GraphQueryDto.Response buildTableResponse(String projectId, List<Map<String, Object>> rows) {
        return GraphQueryDto.Response.builder()
                .projectId(projectId)
                .mode("TABLE")
                .rowCount(rows.size())
                .rows(rows)
                .nodes(List.of())
                .edges(List.of())
                .build();
    }

    /** MERMAID モード: rows から flowchart TD 形式を生成 */
    private GraphQueryDto.Response buildMermaidResponse(String projectId, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder("flowchart TD\n");

        // 使用済み ID を追跡（Mermaid のノード定義は1回だけ）
        Map<String, String> nodeLabels = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String srcId = strVal(row, "sourceId");
            String tgtId = strVal(row, "targetId");
            String srcName = coalesce(strVal(row, "sourceName"), srcId, "unknown");
            String tgtName = coalesce(strVal(row, "targetName"), tgtId, "unknown");
            String relType = strVal(row, "relType");

            if (srcId != null) nodeLabels.putIfAbsent(sanitizeMermaidId(srcId), srcName);
            if (tgtId != null) nodeLabels.putIfAbsent(sanitizeMermaidId(tgtId), tgtName);

            if (srcId != null && tgtId != null) {
                String label = relType != null ? "|" + relType + "|" : "";
                sb.append("    ")
                  .append(sanitizeMermaidId(srcId))
                  .append(" -->").append(label).append(" ")
                  .append(sanitizeMermaidId(tgtId))
                  .append("\n");
            } else {
                // ノード単体（エッジなし）
                String nodeId = strVal(row, "nodeId");
                String nodeName = coalesce(strVal(row, "nodeName"), nodeId, "unknown");
                if (nodeId != null) {
                    nodeLabels.putIfAbsent(sanitizeMermaidId(nodeId), nodeName);
                }
            }
        }

        // ノード定義を先頭に挿入
        StringBuilder def = new StringBuilder("flowchart TD\n");
        nodeLabels.forEach((id, label) ->
            def.append("    ").append(id).append("[\"").append(escapeMermaid(label)).append("\"]\n")
        );
        // エッジ部分を追記（先頭の "flowchart TD\n" を除く）
        def.append(sb.substring("flowchart TD\n".length()));

        return GraphQueryDto.Response.builder()
                .projectId(projectId)
                .mode("MERMAID")
                .rowCount(rows.size())
                .rows(rows)
                .nodes(List.of())
                .edges(List.of())
                .mermaid(def.toString())
                .build();
    }

    private GraphQueryDto.Response buildSequenceResponse(String projectId, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder("sequenceDiagram\n");
        Map<String, String> participants = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String srcId = strVal(row, "sourceId");
            String tgtId = strVal(row, "targetId");
            String srcName = coalesce(strVal(row, "sourceName"), srcId, "unknown");
            String tgtName = coalesce(strVal(row, "targetName"), tgtId, "unknown");
            String relType = coalesce(strVal(row, "relType"), "related");

            if (srcId != null) participants.putIfAbsent(sanitizeMermaidId(srcId), srcName);
            if (tgtId != null) participants.putIfAbsent(sanitizeMermaidId(tgtId), tgtName);
        }

        participants.forEach((id, label) ->
                sb.append("    participant ").append(id).append(" as ").append(escapeMermaid(label)).append("\n"));

        int count = 0;
        for (Map<String, Object> row : rows) {
            String srcId = strVal(row, "sourceId");
            String tgtId = strVal(row, "targetId");
            String relType = coalesce(strVal(row, "relType"), "related");
            if (srcId != null && tgtId != null && count < 80) {
                sb.append("    ")
                        .append(sanitizeMermaidId(srcId))
                        .append("->>")
                        .append(sanitizeMermaidId(tgtId))
                        .append(": ")
                        .append(escapeMermaid(relType))
                        .append("\n");
                count++;
            }
        }

        return GraphQueryDto.Response.builder()
                .projectId(projectId)
                .mode("SEQUENCE")
                .rowCount(rows.size())
                .rows(rows)
                .nodes(List.of())
                .edges(List.of())
                .mermaid(sb.toString())
                .build();
    }

    private void extractNode(Map<String, Object> row, String idKey, String labelKey,
                             String nameKey, String srcKey, List<GraphQueryDto.GraphNode> nodes) {
        String id = strVal(row, idKey);
        if (id == null) return;
        String label = coalesce(strVal(row, labelKey), "Node");
        String name = coalesce(strVal(row, nameKey), id);
        String src = coalesce(strVal(row, srcKey), "");
        nodes.add(GraphQueryDto.GraphNode.builder()
                .id(id).label(label).name(name).sourcePath(src)
                .build());
    }

    private String strVal(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : v.toString();
    }

    private String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private String sanitizeMermaidId(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String escapeMermaid(String s) {
        return s.replace("\"", "'").replace("[", "(").replace("]", ")");
    }

    private Map<String, Object> toSafeMap(Map<String, Object> raw) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (raw == null) return safe;
        raw.forEach((k, v) -> safe.put(k, v == null ? "" : v));
        return safe;
    }
}

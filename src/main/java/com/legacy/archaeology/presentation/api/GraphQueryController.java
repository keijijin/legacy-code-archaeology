package com.legacy.archaeology.presentation.api;

import com.legacy.archaeology.application.dto.GraphQueryDto;
import com.legacy.archaeology.application.usecases.ExecuteGraphQueryUseCase;
import com.legacy.archaeology.infrastructure.graph.GraphQueryExecutor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Neo4j グラフ直接クエリ API。
 * 読み取り専用 Cypher を実行し、GRAPH / TABLE / MERMAID 形式で返す。
 */
@RestController
@RequestMapping("/api/projects/{projectId}/graph")
@RequiredArgsConstructor
public class GraphQueryController {

    private final ExecuteGraphQueryUseCase executeGraphQueryUseCase;
    private final GraphQueryExecutor graphQueryExecutor;

    /**
     * Cypher クエリを実行してグラフデータを返す。
     * mode: GRAPH（ノード+エッジ）/ TABLE（行一覧）/ MERMAID（Mermaid 文字列）
     */
    @PostMapping("/query")
    public ResponseEntity<GraphQueryDto.Response> query(
            @PathVariable String projectId,
            @RequestBody @Valid GraphQueryDto.Request request) {
        // 書き込み系クエリをブロック
        graphQueryExecutor.validateReadOnly(request.getCypher());
        GraphQueryDto.Response response = executeGraphQueryUseCase.execute(projectId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * プロジェクト内の全ノード概要を取得（グラフ一覧表示用）。
     */
    @GetMapping("/nodes")
    public ResponseEntity<GraphQueryDto.Response> nodes(@PathVariable String projectId) {
        GraphQueryDto.Request req = GraphQueryDto.Request.builder()
                .cypher("""
                        MATCH (n)
                        WHERE n.projectId = $projectId
                        RETURN labels(n)[0] AS nodeLabel,
                               n.id AS nodeId,
                               coalesce(n.className, n.routeId, n.tableName, n.name, n.id) AS nodeName,
                               coalesce(n.sourcePath, '') AS nodeSourcePath
                        ORDER BY nodeLabel, nodeName
                        LIMIT 500
                        """)
                .mode("GRAPH")
                .bindProjectId(true)
                .build();
        graphQueryExecutor.validateReadOnly(req.getCypher());
        return ResponseEntity.ok(executeGraphQueryUseCase.execute(projectId, req));
    }

    /**
     * プロジェクト内の全エッジを取得（グラフ描画用）。
     */
    @GetMapping("/edges")
    public ResponseEntity<GraphQueryDto.Response> edges(@PathVariable String projectId) {
        GraphQueryDto.Request req = GraphQueryDto.Request.builder()
                .cypher("""
                        MATCH (a)-[r]->(b)
                        WHERE a.projectId = $projectId AND b.projectId = $projectId
                        RETURN a.id AS sourceId,
                               labels(a)[0] AS sourceLabel,
                               coalesce(a.className, a.routeId, a.tableName, a.name, a.id) AS sourceName,
                               coalesce(a.sourcePath, '') AS sourceSourcePath,
                               b.id AS targetId,
                               labels(b)[0] AS targetLabel,
                               coalesce(b.className, b.routeId, b.tableName, b.name, b.id) AS targetName,
                               coalesce(b.sourcePath, '') AS targetSourcePath,
                               type(r) AS relType
                        LIMIT 1000
                        """)
                .mode("GRAPH")
                .bindProjectId(true)
                .build();
        graphQueryExecutor.validateReadOnly(req.getCypher());
        return ResponseEntity.ok(executeGraphQueryUseCase.execute(projectId, req));
    }

    /**
     * Mermaid 形式でプロジェクト全体のグラフを返す。
     */
    @GetMapping("/mermaid")
    public ResponseEntity<GraphQueryDto.Response> mermaid(@PathVariable String projectId) {
        GraphQueryDto.Request req = GraphQueryDto.Request.builder()
                .cypher("""
                        MATCH (a)-[r]->(b)
                        WHERE a.projectId = $projectId AND b.projectId = $projectId
                        RETURN a.id AS sourceId,
                               coalesce(a.className, a.routeId, a.tableName, a.name, a.id) AS sourceName,
                               b.id AS targetId,
                               coalesce(b.className, b.routeId, b.tableName, b.name, b.id) AS targetName,
                               type(r) AS relType
                        LIMIT 200
                        """)
                .mode("MERMAID")
                .bindProjectId(true)
                .build();
        graphQueryExecutor.validateReadOnly(req.getCypher());
        return ResponseEntity.ok(executeGraphQueryUseCase.execute(projectId, req));
    }

    /**
     * sequenceDiagram 形式でプロジェクト関係を返す。
     */
    @GetMapping("/sequence")
    public ResponseEntity<GraphQueryDto.Response> sequence(@PathVariable String projectId) {
        GraphQueryDto.Request req = GraphQueryDto.Request.builder()
                .cypher("""
                        MATCH (a)-[r]->(b)
                        WHERE a.projectId = $projectId AND b.projectId = $projectId
                        RETURN a.id AS sourceId,
                               labels(a)[0] AS sourceLabel,
                               coalesce(a.className, a.routeId, a.tableName, a.name, a.id) AS sourceName,
                               b.id AS targetId,
                               labels(b)[0] AS targetLabel,
                               coalesce(b.className, b.routeId, b.tableName, b.name, b.id) AS targetName,
                               type(r) AS relType
                        LIMIT 120
                        """)
                .mode("SEQUENCE")
                .bindProjectId(true)
                .build();
        graphQueryExecutor.validateReadOnly(req.getCypher());
        return ResponseEntity.ok(executeGraphQueryUseCase.execute(projectId, req));
    }
}

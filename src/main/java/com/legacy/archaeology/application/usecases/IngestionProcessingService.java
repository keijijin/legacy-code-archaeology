package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.domain.assets.AssetEntity;
import com.legacy.archaeology.domain.assets.AssetType;
import com.legacy.archaeology.domain.knowledge.EvidenceEntity;
import com.legacy.archaeology.domain.knowledge.EvidenceRepository;
import com.legacy.archaeology.infrastructure.graph.GraphSyncService;
import com.legacy.archaeology.infrastructure.ir.IrMapper;
import com.legacy.archaeology.infrastructure.ir.ProgramIr;
import com.legacy.archaeology.infrastructure.ir.RouteIr;
import com.legacy.archaeology.infrastructure.ir.TableIr;
import com.legacy.archaeology.infrastructure.parser.CamelRouteParser;
import com.legacy.archaeology.infrastructure.parser.JavaSourceParser;
import com.legacy.archaeology.infrastructure.parser.SqlDdlParser;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 取込資産を同期的にパースし、IR変換・知識グラフ反映・Evidence生成を行う処理サービス。
 * MVP段階の非同期ワーカー未接続を補い、取込時に成果物を生成する。
 * ソースは実ファイル、なければクラスパス（samples同梱）から解決する。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionProcessingService {

    private final JavaSourceParser javaSourceParser;
    private final CamelRouteParser camelRouteParser;
    private final SqlDdlParser sqlDdlParser;
    private final IrMapper irMapper;
    private final GraphSyncService graphSyncService;
    private final EvidenceRepository evidenceRepository;
    private final IdGenerator idGenerator;

    /** 資産の内容を読み込めた場合のみ、パース→IR→グラフ→Evidenceを実行する。 */
    public void process(AssetEntity asset) {
        String projectId = asset.getProjectId();
        String assetId = asset.getAssetId();
        String sourcePath = asset.getSourcePath();

        String content = readContent(sourcePath);
        if (content == null) {
            log.warn("取込ソースを読み込めませんでした（スキップ）: {}", sourcePath);
            return;
        }

        try {
            switch (asset.getAssetType()) {
                case JAVA_SOURCE -> processJava(projectId, assetId, sourcePath, content);
                case CAMEL_ROUTE -> processCamel(projectId, assetId, sourcePath, content);
                case SQL_DDL -> processSql(projectId, assetId, sourcePath, content);
                case YAML_CONFIG, PROPERTIES_CONFIG ->
                        processConfig(projectId, assetId, sourcePath, content);
                default -> log.info("解析対象外の資産種別: {}", asset.getAssetType());
            }
            graphSyncService.linkRoutesToProgramsAndTables(projectId);
        } catch (Exception e) {
            log.error("取込処理中にエラー sourcePath={}", sourcePath, e);
        }
    }

    private void processJava(String projectId, String assetId, String sourcePath, String content) {
        Path temp = writeTemp(content, ".java");
        if (temp == null) return;
        JavaSourceParser.ParsedJavaUnit unit = javaSourceParser.parse(temp);
        List<ProgramIr> programs = irMapper.fromJava(unit, projectId, assetId);
        graphSyncService.syncPrograms(programs);
        for (ProgramIr ir : programs) {
            saveEvidence(projectId, assetId, sourcePath, "JAVA_CLASS",
                    "class " + ir.getClassName() + " (" + ir.getPackageName() + ")");
            ir.getMethods().forEach(m ->
                    saveEvidence(projectId, assetId, sourcePath, "JAVA_METHOD",
                            "method " + m.getName() + " returns " + m.getReturnType()));
        }
        log.info("Java取込処理完了 assetId={} programs={}", assetId, programs.size());
    }

    private void processCamel(String projectId, String assetId, String sourcePath, String content) {
        List<CamelRouteParser.ParsedRoute> routes =
                camelRouteParser.parseContent(content, sourcePath);
        List<RouteIr> routeIrs = irMapper.fromCamel(routes, projectId, assetId);
        graphSyncService.syncRoutes(routeIrs);
        for (RouteIr ir : routeIrs) {
            saveEvidence(projectId, assetId, sourcePath, "CAMEL_ROUTE",
                    "route " + ir.getRouteId() + " from " + ir.getFromUri()
                            + " steps=" + ir.getSteps());
        }
        log.info("Camel取込処理完了 assetId={} routes={}", assetId, routeIrs.size());
    }

    private void processSql(String projectId, String assetId, String sourcePath, String content) {
        List<SqlDdlParser.ParsedTable> tables = sqlDdlParser.parse(content);
        List<TableIr> tableIrs = irMapper.fromSql(tables, projectId, assetId, sourcePath);
        graphSyncService.syncTables(tableIrs);
        for (TableIr ir : tableIrs) {
            saveEvidence(projectId, assetId, sourcePath, "SQL_TABLE",
                    "table " + ir.getTableName()
                            + " columns=" + ir.getColumns().stream().map(TableIr.ColumnIr::getName).toList());
        }
        log.info("SQL取込処理完了 assetId={} tables={}", assetId, tableIrs.size());
    }

    private void processConfig(String projectId, String assetId, String sourcePath, String content) {
        saveEvidence(projectId, assetId, sourcePath, "CONFIG",
                content.length() > 2000 ? content.substring(0, 2000) : content);
        log.info("設定資産取込処理完了 assetId={}", assetId);
    }

    private void saveEvidence(
            String projectId, String assetId, String sourcePath, String type, String snippet) {
        EvidenceEntity ev = new EvidenceEntity();
        ev.setEvidenceId(idGenerator.generateEvidenceId());
        ev.setProjectId(projectId);
        ev.setSourceAssetId(assetId);
        ev.setSourcePath(sourcePath);
        ev.setEvidenceType(type);
        ev.setSnippet(snippet);
        evidenceRepository.save(ev);
    }

    private String readContent(String sourcePath) {
        try {
            Path p = Path.of(sourcePath);
            if (Files.exists(p)) {
                return Files.readString(p, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // フォールスルー
        }
        // クラスパス（samples 同梱）から解決
        String normalizedPath = sourcePath == null ? "" : sourcePath.replaceFirst("^classpath:", "");
        String[] candidates = {
                normalizedPath,
                "samples/" + normalizedPath.replaceFirst("^samples/", "")
        };
        for (String cp : candidates) {
            try {
                ClassPathResource res = new ClassPathResource(cp);
                if (res.exists()) {
                    try (InputStream is = res.getInputStream()) {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception ignored) {
                // 次候補へ
            }
        }
        return null;
    }

    private Path writeTemp(String content, String suffix) {
        try {
            Path temp = Files.createTempFile("lca-ingest-", suffix);
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            temp.toFile().deleteOnExit();
            return temp;
        } catch (IOException e) {
            log.error("一時ファイル作成失敗", e);
            return null;
        }
    }
}

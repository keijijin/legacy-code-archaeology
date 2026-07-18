package com.legacy.archaeology.infrastructure.ir;

import com.legacy.archaeology.infrastructure.parser.CamelRouteParser;
import com.legacy.archaeology.infrastructure.parser.JavaSourceParser;
import com.legacy.archaeology.infrastructure.parser.SqlDdlParser;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * パーサ結果を中間表現（IR）へ変換するマッパー。
 * 言語別の差異をここで吸収し、上位レイヤへは共通IRのみを渡す。
 */
@Component
@RequiredArgsConstructor
public class IrMapper {

    private static final String PARSER_VERSION = "1.0.0";
    private final IdGenerator idGenerator;

    public List<ProgramIr> fromJava(
            JavaSourceParser.ParsedJavaUnit unit, String projectId, String sourceAssetId) {

        return unit.getClasses().stream()
                .map(cls ->
                        ProgramIr.builder()
                                .id(idGenerator.generateProjectId().replace("PRJ-", "ENT-"))
                                .projectId(projectId)
                                .sourceAssetId(sourceAssetId)
                                .sourcePath(unit.getSourcePath())
                                .language("Java")
                                .packageName(unit.getPackageName())
                                .className(cls.getName())
                                .isInterface(cls.isInterface())
                                .lineNumber(cls.getLineNumber())
                                .methods(
                                        cls.getMethods().stream()
                                                .map(m ->
                                                        ProgramIr.MethodIr.builder()
                                                                .name(m.getName())
                                                                .returnType(m.getReturnType())
                                                                .lineNumber(m.getLineNumber())
                                                                .build())
                                                .toList())
                                .parserVersion(PARSER_VERSION)
                                .build())
                .toList();
    }

    public List<RouteIr> fromCamel(
            List<CamelRouteParser.ParsedRoute> routes, String projectId, String sourceAssetId) {

        return routes.stream()
                .map(r ->
                        RouteIr.builder()
                                .id(idGenerator.generateProjectId().replace("PRJ-", "ENT-"))
                                .projectId(projectId)
                                .sourceAssetId(sourceAssetId)
                                .sourcePath(r.getSourcePath())
                                .routeId(r.getRouteId())
                                .fromUri(r.getFromUri())
                                .steps(r.getSteps())
                                .parserVersion(PARSER_VERSION)
                                .build())
                .toList();
    }

    public List<TableIr> fromSql(
            List<SqlDdlParser.ParsedTable> tables, String projectId, String sourceAssetId,
            String sourcePath) {

        return tables.stream()
                .map(t ->
                        TableIr.builder()
                                .id(idGenerator.generateProjectId().replace("PRJ-", "ENT-"))
                                .projectId(projectId)
                                .sourceAssetId(sourceAssetId)
                                .sourcePath(sourcePath)
                                .tableName(t.getTableName())
                                .columns(
                                        t.getColumns().stream()
                                                .map(c ->
                                                        TableIr.ColumnIr.builder()
                                                                .name(c.getName())
                                                                .dataType(c.getDataType())
                                                                .build())
                                                .toList())
                                .parserVersion(PARSER_VERSION)
                                .build())
                .toList();
    }
}

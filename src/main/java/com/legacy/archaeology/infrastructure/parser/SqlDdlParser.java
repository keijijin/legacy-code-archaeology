package com.legacy.archaeology.infrastructure.parser;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.stereotype.Component;

/**
 * SQL DDLパーサ。
 * CREATE TABLE文を解析してテーブル名・カラム定義を抽出する。
 */
@Component
@Slf4j
public class SqlDdlParser {

    /**
     * DDL文字列を解析し、テーブル定義一覧を返す。
     */
    public List<ParsedTable> parse(String ddlContent) {
        List<ParsedTable> tables = new ArrayList<>();
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(ddlContent);
            for (Statement statement : statements.getStatements()) {
                if (statement instanceof CreateTable createTable) {
                    tables.add(extractTable(createTable));
                }
            }
            log.debug("DDL解析完了 tables={}", tables.size());
        } catch (Exception e) {
            log.warn("DDL解析エラー: {}", e.getMessage());
        }
        return tables;
    }

    private ParsedTable extractTable(CreateTable createTable) {
        String tableName = createTable.getTable().getName();
        List<ParsedColumn> columns = new ArrayList<>();

        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition col : createTable.getColumnDefinitions()) {
                columns.add(
                        ParsedColumn.builder()
                                .name(col.getColumnName())
                                .dataType(col.getColumnSpecs() != null
                                        ? String.join(" ", col.getColumnSpecs())
                                        : col.getColumnSpecs() == null
                                                ? ""
                                                : "")
                                .build());
            }
        }

        return ParsedTable.builder()
                .tableName(tableName)
                .columns(columns)
                .build();
    }

    /** 解析結果: テーブル */
    @lombok.Value
    @lombok.Builder
    public static class ParsedTable {
        String tableName;
        List<ParsedColumn> columns;
    }

    /** 解析結果: カラム */
    @lombok.Value
    @lombok.Builder
    public static class ParsedColumn {
        String name;
        String dataType;
    }
}

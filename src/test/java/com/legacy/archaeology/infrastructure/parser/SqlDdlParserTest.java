package com.legacy.archaeology.infrastructure.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlDdlParserTest {

    private final SqlDdlParser parser = new SqlDdlParser();

    @Test
    void CREATE_TABLEからテーブル名とカラムを抽出できること() {
        String ddl =
                """
                CREATE TABLE customers (
                    id BIGSERIAL PRIMARY KEY,
                    customer_id VARCHAR(20) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL
                );
                """;

        List<SqlDdlParser.ParsedTable> tables = parser.parse(ddl);

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).getTableName()).isEqualToIgnoringCase("customers");
        assertThat(tables.get(0).getColumns()).extracting("name")
                .containsExactlyInAnyOrder("id", "customer_id", "name", "created_at");
    }

    @Test
    void 不正なDDLでも例外を投げないこと() {
        List<SqlDdlParser.ParsedTable> tables = parser.parse("NOT A VALID SQL;");
        assertThat(tables).isEmpty();
    }
}

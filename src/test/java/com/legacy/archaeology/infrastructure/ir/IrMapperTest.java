package com.legacy.archaeology.infrastructure.ir;

import static org.assertj.core.api.Assertions.assertThat;

import com.legacy.archaeology.infrastructure.parser.JavaSourceParser;
import com.legacy.archaeology.infrastructure.parser.SqlDdlParser;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.util.List;
import org.junit.jupiter.api.Test;

class IrMapperTest {

    private final IrMapper mapper = new IrMapper(new IdGenerator());

    @Test
    void JavaパーサからProgramIrへ変換できること() {
        JavaSourceParser.ParsedJavaUnit unit = JavaSourceParser.ParsedJavaUnit.builder()
                .sourcePath("src/CustomerService.java")
                .packageName("com.example")
                .classes(List.of(
                        JavaSourceParser.ParsedClass.builder()
                                .name("CustomerService")
                                .isInterface(false)
                                .lineNumber(5)
                                .methods(List.of())
                                .build()))
                .build();

        List<ProgramIr> programs = mapper.fromJava(unit, "PRJ-001", "AST-001");

        assertThat(programs).hasSize(1);
        assertThat(programs.get(0).getClassName()).isEqualTo("CustomerService");
        assertThat(programs.get(0).getProjectId()).isEqualTo("PRJ-001");
        assertThat(programs.get(0).getId()).startsWith("ENT-");
    }

    @Test
    void DDLパーサからTableIrへ変換できること() {
        SqlDdlParser.ParsedTable table = SqlDdlParser.ParsedTable.builder()
                .tableName("customers")
                .columns(List.of(
                        SqlDdlParser.ParsedColumn.builder()
                                .name("id").dataType("BIGSERIAL").build(),
                        SqlDdlParser.ParsedColumn.builder()
                                .name("name").dataType("VARCHAR(255)").build()))
                .build();

        List<TableIr> tables = mapper.fromSql(List.of(table), "PRJ-001", "AST-002", "schema.sql");

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).getTableName()).isEqualTo("customers");
        assertThat(tables.get(0).getColumns()).hasSize(2);
    }
}

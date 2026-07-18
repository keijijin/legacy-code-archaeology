package com.legacy.archaeology.regression;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacy.archaeology.infrastructure.parser.CamelRouteParser;
import com.legacy.archaeology.infrastructure.parser.JavaSourceParser;
import com.legacy.archaeology.infrastructure.parser.SqlDdlParser;
import com.legacy.archaeology.infrastructure.parser.YamlConfigParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * 既知フィクスチャに対する解析結果検証テスト。
 * 自由文一致ではなく、構造化項目単位で評価する。
 */
class FixtureAnalysisVerificationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JavaSourceParser javaSourceParser = new JavaSourceParser();
    private final CamelRouteParser camelRouteParser = new CamelRouteParser();
    private final SqlDdlParser sqlDdlParser = new SqlDdlParser();
    private final YamlConfigParser yamlConfigParser = new YamlConfigParser();

    @Test
    void Javaフィクスチャのクラスとメソッドが期待どおり抽出されること() throws Exception {
        Path javaFile = copyToTemp("fixtures/java/CustomerRegistrationService.java", ".java");
        JsonNode expected = readJson("fixtures/expected/java_customer_registration.json");

        JavaSourceParser.ParsedJavaUnit unit = javaSourceParser.parse(javaFile);

        assertThat(unit.getPackageName()).isEqualTo(expected.get("packageName").asText());
        assertThat(unit.getClasses()).hasSize(1);
        assertThat(unit.getClasses().get(0).getName()).isEqualTo(expected.get("className").asText());
        assertThat(unit.getClasses().get(0).getMethods())
                .extracting(JavaSourceParser.ParsedMethod::getName)
                .containsExactlyInAnyOrderElementsOf(
                        objectMapper.convertValue(
                                expected.get("methodNames"),
                                objectMapper
                                        .getTypeFactory()
                                        .constructCollectionType(List.class, String.class)));
        assertThat(unit.getClasses().get(0).getMethods())
                .hasSize(expected.get("methodCount").asInt());
    }

    @Test
    void CamelフィクスチャのRouteが期待どおり抽出されること() throws Exception {
        String xml =
                Files.readString(
                        Path.of(
                                new ClassPathResource(
                                                "fixtures/camel/customer-registration-route.xml")
                                        .getURI()));
        JsonNode expected = readJson("fixtures/expected/camel_customer_route.json");

        List<CamelRouteParser.ParsedRoute> routes =
                camelRouteParser.parseContent(xml, "customer-registration-route.xml");

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getRouteId()).isEqualTo(expected.get("routeId").asText());
        assertThat(routes.get(0).getFromUri()).isEqualTo(expected.get("fromUri").asText());
        assertThat(routes.get(0).getSteps())
                .containsExactlyElementsOf(
                        objectMapper.convertValue(
                                expected.get("steps"),
                                objectMapper
                                        .getTypeFactory()
                                        .constructCollectionType(List.class, String.class)));
    }

    @Test
    void SQLフィクスチャのテーブルとカラムが期待どおり抽出されること() throws Exception {
        String ddl =
                Files.readString(
                        Path.of(new ClassPathResource("fixtures/sql/customer_schema.sql").getURI()));
        JsonNode expected = readJson("fixtures/expected/sql_customer_schema.json");

        List<SqlDdlParser.ParsedTable> tables = sqlDdlParser.parse(ddl);

        assertThat(tables).hasSize(expected.get("tables").size());
        for (JsonNode tableNode : expected.get("tables")) {
            String tableName = tableNode.get("tableName").asText();
            SqlDdlParser.ParsedTable actual =
                    tables.stream()
                            .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                            .findFirst()
                            .orElseThrow();
            assertThat(actual.getColumns())
                    .extracting(SqlDdlParser.ParsedColumn::getName)
                    .containsExactlyInAnyOrderElementsOf(
                            objectMapper.convertValue(
                                    tableNode.get("columns"),
                                    objectMapper
                                            .getTypeFactory()
                                            .constructCollectionType(List.class, String.class)));
        }
    }

    @Test
    void YAMLフィクスチャから接続設定キーが抽出されること() throws Exception {
        Path yamlFile = copyToTemp("fixtures/yaml/application-sample.yml", ".yml");
        Map<String, String> flat = yamlConfigParser.parse(yamlFile);

        assertThat(flat).containsKey("spring.datasource.url");
        assertThat(flat.get("spring.datasource.url")).contains("localhost");
        assertThat(flat).containsKey("integration.notification.endpoint");
        assertThat(flat).containsKey("integration.file.input-dir");
    }

    private JsonNode readJson(String classpath) throws Exception {
        return objectMapper.readTree(new ClassPathResource(classpath).getInputStream());
    }

    private Path copyToTemp(String classpath, String suffix) throws Exception {
        Path temp = Files.createTempFile("lca-fixture-", suffix);
        Files.write(temp, new ClassPathResource(classpath).getInputStream().readAllBytes());
        temp.toFile().deleteOnExit();
        return temp;
    }
}

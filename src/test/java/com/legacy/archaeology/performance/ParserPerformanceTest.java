package com.legacy.archaeology.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.legacy.archaeology.infrastructure.parser.CamelRouteParser;
import com.legacy.archaeology.infrastructure.parser.JavaSourceParser;
import com.legacy.archaeology.infrastructure.parser.SqlDdlParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * パーサ性能の簡易計測テスト。
 * 中規模相当（反復解析）で一定時間内に完了することを確認する。
 */
class ParserPerformanceTest {

    private static final long MAX_MILLIS = 5_000L;

    @Test
    void フィクスチャの反復解析が制限時間内に完了すること() throws Exception {
        JavaSourceParser javaParser = new JavaSourceParser();
        CamelRouteParser camelParser = new CamelRouteParser();
        SqlDdlParser sqlParser = new SqlDdlParser();

        Path javaFile = Files.createTempFile("perf-java-", ".java");
        Files.write(
                javaFile,
                new ClassPathResource("fixtures/java/CustomerRegistrationService.java")
                        .getInputStream()
                        .readAllBytes());
        String camelXml =
                Files.readString(
                        Path.of(
                                new ClassPathResource(
                                                "fixtures/camel/customer-registration-route.xml")
                                        .getURI()));
        String ddl =
                Files.readString(
                        Path.of(new ClassPathResource("fixtures/sql/customer_schema.sql").getURI()));

        long start = System.currentTimeMillis();
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            counts.add(javaParser.parse(javaFile).getClasses().size());
            counts.add(camelParser.parseContent(camelXml, "route.xml").size());
            counts.add(sqlParser.parse(ddl).size());
        }
        long elapsed = System.currentTimeMillis() - start;

        assertThat(counts).isNotEmpty();
        assertThat(elapsed)
                .as("parser loop should finish within %d ms but was %d ms", MAX_MILLIS, elapsed)
                .isLessThan(MAX_MILLIS);

        javaFile.toFile().deleteOnExit();
    }
}

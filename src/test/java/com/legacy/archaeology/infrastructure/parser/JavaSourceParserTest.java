package com.legacy.archaeology.infrastructure.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    void Javaクラスのメソッドを抽出できること(@TempDir Path tempDir) throws IOException {
        String source =
                """
                package com.example;
                public class CustomerService {
                    public void registerCustomer(String name) {}
                    public boolean isValidCustomer(String id) { return true; }
                }
                """;

        Path file = tempDir.resolve("CustomerService.java");
        Files.writeString(file, source);

        JavaSourceParser.ParsedJavaUnit unit = parser.parse(file);

        assertThat(unit.getPackageName()).isEqualTo("com.example");
        assertThat(unit.getClasses()).hasSize(1);
        assertThat(unit.getClasses().get(0).getName()).isEqualTo("CustomerService");
        assertThat(unit.getClasses().get(0).getMethods()).hasSize(2);
    }

    @Test
    void 存在しないファイルを解析しても例外を投げないこと() {
        JavaSourceParser.ParsedJavaUnit unit = parser.parse(Path.of("/nonexistent/File.java"));
        assertThat(unit.getClasses()).isEmpty();
    }
}

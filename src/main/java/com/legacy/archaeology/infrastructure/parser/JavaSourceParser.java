package com.legacy.archaeology.infrastructure.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Javaソースコードの静的解析パーサ。
 * ASTを用いてクラス・メソッド・呼び出し関係を抽出し、中間表現へ変換する。
 */
@Component
@Slf4j
public class JavaSourceParser {

    private final JavaParser javaParser = new JavaParser();

    /**
     * Javaファイルを解析し、解析結果を返す。
     */
    public ParsedJavaUnit parse(Path filePath) {
        try {
            ParseResult<CompilationUnit> result = javaParser.parse(filePath);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                log.warn("Java解析失敗: {}", filePath);
                return ParsedJavaUnit.empty(filePath.toString());
            }

            CompilationUnit cu = result.getResult().get();
            return extractUnit(cu, filePath.toString());

        } catch (IOException e) {
            log.error("Javaファイル読み込みエラー: {}", filePath, e);
            return ParsedJavaUnit.empty(filePath.toString());
        }
    }

    private ParsedJavaUnit extractUnit(CompilationUnit cu, String sourcePath) {
        List<ParsedClass> classes = new ArrayList<>();

        cu.findAll(ClassOrInterfaceDeclaration.class)
                .forEach(
                        cls -> {
                            List<ParsedMethod> methods = new ArrayList<>();
                            cls.findAll(MethodDeclaration.class)
                                    .forEach(
                                            m ->
                                                    methods.add(
                                                            ParsedMethod.builder()
                                                                    .name(m.getNameAsString())
                                                                    .returnType(
                                                                            m.getTypeAsString())
                                                                    .lineNumber(
                                                                            m.getBegin()
                                                                                    .map(
                                                                                            p ->
                                                                                                    p.line)
                                                                                    .orElse(0))
                                                                    .build()));

                            classes.add(
                                    ParsedClass.builder()
                                            .name(cls.getNameAsString())
                                            .isInterface(cls.isInterface())
                                            .methods(methods)
                                            .lineNumber(
                                                    cls.getBegin()
                                                            .map(p -> p.line)
                                                            .orElse(0))
                                            .build());
                        });

        String packageName =
                cu.getPackageDeclaration()
                        .map(p -> p.getNameAsString())
                        .orElse("(default)");

        log.debug("Java解析完了 sourcePath={} classes={}", sourcePath, classes.size());

        return ParsedJavaUnit.builder()
                .sourcePath(sourcePath)
                .packageName(packageName)
                .classes(classes)
                .build();
    }

    /** 解析結果: コンパイルユニット */
    @lombok.Value
    @lombok.Builder
    public static class ParsedJavaUnit {
        String sourcePath;
        String packageName;
        List<ParsedClass> classes;

        public static ParsedJavaUnit empty(String sourcePath) {
            return ParsedJavaUnit.builder()
                    .sourcePath(sourcePath)
                    .packageName("(unknown)")
                    .classes(List.of())
                    .build();
        }
    }

    /** 解析結果: クラス */
    @lombok.Value
    @lombok.Builder
    public static class ParsedClass {
        String name;
        boolean isInterface;
        int lineNumber;
        List<ParsedMethod> methods;
    }

    /** 解析結果: メソッド */
    @lombok.Value
    @lombok.Builder
    public static class ParsedMethod {
        String name;
        String returnType;
        int lineNumber;
    }
}

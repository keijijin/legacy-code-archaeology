package com.legacy.archaeology.infrastructure.ir;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 中間表現: プログラム（クラス・関数）。
 * 言語固有構造を吸収し、知識グラフへ投入できる共通形式で保持する。
 */
@Value
@Builder
public class ProgramIr {

    String id;              // ENT-xxxx
    String projectId;
    String sourceAssetId;
    String sourcePath;
    String language;        // Java / Camel / SQL / Shell
    String packageName;
    String className;
    boolean isInterface;
    int lineNumber;
    List<MethodIr> methods;
    String parserVersion;

    @Value
    @Builder
    public static class MethodIr {
        String name;
        String returnType;
        int lineNumber;
    }
}

package com.legacy.archaeology.infrastructure.ir;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 中間表現: Apache Camel Route。
 * from / to / bean / log 等のステップを共通形式で保持する。
 */
@Value
@Builder
public class RouteIr {

    String id;              // ENT-xxxx
    String projectId;
    String sourceAssetId;
    String sourcePath;
    String routeId;
    String fromUri;
    List<String> steps;
    String parserVersion;
}

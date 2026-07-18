package com.legacy.archaeology.infrastructure.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Apache Camel RouteのXMLパーサ。
 * from/to/bean/log などの主要ステップを抽出する。
 */
@Component
@Slf4j
public class CamelRouteParser {

    /**
     * Camel XML設定ファイルを解析し、Route定義一覧を返す。
     */
    public List<ParsedRoute> parse(Path filePath) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return parseContent(content, filePath.toString());
        } catch (IOException e) {
            log.error("Camelルートファイル読み込みエラー: {}", filePath, e);
            return List.of();
        }
    }

    public List<ParsedRoute> parseContent(String xmlContent, String sourcePath) {
        List<ParsedRoute> routes = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            NodeList routeNodes = doc.getElementsByTagNameNS("*", "route");
            for (int i = 0; i < routeNodes.getLength(); i++) {
                Element routeEl = (Element) routeNodes.item(i);
                routes.add(extractRoute(routeEl, sourcePath));
            }

            log.debug("Camelルート解析完了 sourcePath={} routes={}", sourcePath, routes.size());
        } catch (Exception e) {
            log.warn("Camelルート解析エラー sourcePath={}: {}", sourcePath, e.getMessage());
        }
        return routes;
    }

    private ParsedRoute extractRoute(Element routeEl, String sourcePath) {
        String routeId = routeEl.getAttribute("id");
        String fromUri = "";
        List<String> steps = new ArrayList<>();

        NodeList children = routeEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element el) {
                String tag = el.getLocalName();
                if (tag == null) continue;
                switch (tag) {
                    case "from" -> fromUri = el.getAttribute("uri");
                    case "to" -> steps.add("to:" + el.getAttribute("uri"));
                    case "bean" -> steps.add("bean:" + el.getAttribute("ref"));
                    case "log" -> steps.add("log:" + el.getAttribute("message"));
                    default -> steps.add(tag);
                }
            }
        }

        return ParsedRoute.builder()
                .routeId(routeId.isBlank() ? "(unnamed-" + sourcePath + ")" : routeId)
                .fromUri(fromUri)
                .steps(steps)
                .sourcePath(sourcePath)
                .build();
    }

    /** 解析結果: Camelルート */
    @lombok.Value
    @lombok.Builder
    public static class ParsedRoute {
        String routeId;
        String fromUri;
        List<String> steps;
        String sourcePath;
    }
}

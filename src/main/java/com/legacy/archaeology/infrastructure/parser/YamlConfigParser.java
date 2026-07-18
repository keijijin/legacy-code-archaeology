package com.legacy.archaeology.infrastructure.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * application.yml / application.properties パーサ。
 * 設定値を Key-Value で抽出し、接続先・外部エンドポイントの証拠として利用する。
 */
@Component
@Slf4j
public class YamlConfigParser {

    @SuppressWarnings("unchecked")
    public Map<String, String> parse(Path filePath) {
        Map<String, String> flat = new HashMap<>();
        try {
            String content = Files.readString(filePath);
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(content);
            if (loaded instanceof Map<?, ?> map) {
                flatten("", (Map<String, Object>) map, flat);
            }
            log.debug("YAML解析完了 sourcePath={} keys={}", filePath, flat.size());
        } catch (IOException e) {
            log.error("YAML読み込みエラー: {}", filePath, e);
        }
        return flat;
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> map, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> nested) {
                flatten(key, (Map<String, Object>) nested, result);
            } else {
                result.put(key, entry.getValue() == null ? "" : entry.getValue().toString());
            }
        }
    }
}

package com.pta.outsourcing.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationLogSanitizer {

    private static final int MAX_LENGTH = 1000;
    private static final String MASK = "******";
    private static final Pattern JSON_PAIR_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|token|authorization)\"\\s*:\\s*\")[^\",}\\]]*");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)((?:password|token|authorization)\\s*=)[^,}&\\s]+");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._~+\\-/]+=*");

    private final ObjectMapper objectMapper;

    /**
     * 先尽量按 JSON 结构脱敏，再按文本格式兜底，最后统一截断入库长度。
     *
     * @param value 原始操作日志参数。
     * @return 脱敏且长度受控的参数。
     */
    public String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String masked = maskJson(value);
        masked = JSON_PAIR_PATTERN.matcher(masked).replaceAll("$1" + MASK);
        masked = KEY_VALUE_PATTERN.matcher(masked).replaceAll("$1" + MASK);
        masked = BEARER_PATTERN.matcher(masked).replaceAll("$1" + MASK);
        return truncate(masked);
    }

    private String maskJson(String value) {
        try {
            JsonNode root = objectMapper.readTree(value);
            maskJsonNode(root, null);
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return value;
        }
    }

    private void maskJsonNode(JsonNode node, String fieldName) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveField(field.getKey())) {
                    objectNode.put(field.getKey(), MASK);
                } else {
                    maskJsonNode(field.getValue(), field.getKey());
                }
            }
            return;
        }
        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(child -> maskJsonNode(child, fieldName));
        }
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase();
        return "password".equals(normalized)
                || "authorization".equals(normalized)
                || normalized.contains("token");
    }

    private String truncate(String value) {
        if (value.length() <= MAX_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LENGTH);
    }
}

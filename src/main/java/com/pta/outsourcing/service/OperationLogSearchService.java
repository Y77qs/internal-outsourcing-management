package com.pta.outsourcing.service;

import com.pta.outsourcing.entity.OperationLog;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class OperationLogSearchService {

    private static final int MAX_SEARCH_LIMIT = 1000;
    private static final ParameterizedTypeReference<Map<String, Object>> SEARCH_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final List<String> SEARCH_FIELDS = List.of(
            "operatorName",
            "moduleName",
            "operationType",
            "requestPath",
            "requestParams",
            "errorMessage"
    );

    private final RestClient restClient;
    private final boolean enabled;
    private final String indexName;

    public OperationLogSearchService(
            RestClient.Builder restClientBuilder,
            @Value("${app.elasticsearch.enabled:true}") boolean enabled,
            @Value("${app.elasticsearch.url:http://localhost:9200}") String url,
            @Value("${app.elasticsearch.index-name:pta-operation-logs}") String indexName
    ) {
        this.restClient = restClientBuilder.baseUrl(url).build();
        this.enabled = enabled;
        this.indexName = indexName;
    }

    /**
     * 同步到 ES 索引；调用方负责将该步骤作为 best-effort 增强处理。
     */
    public void index(OperationLog logEntity) {
        if (!enabled || logEntity.getId() == null) {
            return;
        }
        restClient.put()
                .uri("/{index}/_doc/{id}", indexName, logEntity.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(toDocument(logEntity))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * 使用 Elasticsearch 做关键词候选检索，返回候选操作日志 ID。
     */
    public List<Long> searchIds(String keyword, int limit) {
        if (!enabled || !StringUtils.hasText(keyword) || limit <= 0) {
            return List.of();
        }
        int searchLimit = Math.min(limit, MAX_SEARCH_LIMIT);
        Map<String, Object> response = restClient.post()
                .uri("/{index}/_search", indexName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toSearchRequest(keyword, searchLimit))
                .retrieve()
                .body(SEARCH_RESPONSE_TYPE);
        return extractIds(response);
    }

    private Map<String, Object> toSearchRequest(String keyword, int limit) {
        Map<String, Object> multiMatch = new LinkedHashMap<>();
        multiMatch.put("query", keyword);
        multiMatch.put("fields", SEARCH_FIELDS);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("multi_match", multiMatch);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("size", limit);
        request.put("_source", List.of("id"));
        request.put("query", query);
        return request;
    }

    private List<Long> extractIds(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object hitsNode = response.get("hits");
        if (!(hitsNode instanceof Map<?, ?> hitsMap)) {
            return List.of();
        }
        Object hitListNode = hitsMap.get("hits");
        if (!(hitListNode instanceof List<?> hitList)) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Object hitNode : hitList) {
            Long id = extractId(hitNode);
            if (id != null) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private Long extractId(Object hitNode) {
        if (!(hitNode instanceof Map<?, ?> hitMap)) {
            return null;
        }
        Object sourceNode = hitMap.get("_source");
        if (sourceNode instanceof Map<?, ?> sourceMap) {
            Long sourceId = parseId(sourceMap.get("id"));
            if (sourceId != null) {
                return sourceId;
            }
        }
        return parseId(hitMap.get("_id"));
    }

    private Long parseId(Object idNode) {
        if (idNode instanceof Number number) {
            return number.longValue();
        }
        if (idNode instanceof String text) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> toDocument(OperationLog logEntity) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", logEntity.getId());
        document.put("operatorId", logEntity.getOperatorId());
        document.put("operatorName", logEntity.getOperatorName());
        document.put("moduleName", logEntity.getModuleName());
        document.put("operationType", logEntity.getOperationType());
        document.put("requestPath", logEntity.getRequestPath());
        document.put("requestParams", logEntity.getRequestParams());
        document.put("result", logEntity.getResult());
        document.put("errorMessage", logEntity.getErrorMessage());
        document.put("createdAt", logEntity.getCreatedAt() == null ? null : logEntity.getCreatedAt().toString());
        return document;
    }
}

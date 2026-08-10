package com.pta.outsourcing.service;

import com.pta.outsourcing.entity.OperationLog;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OperationLogSearchService {

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

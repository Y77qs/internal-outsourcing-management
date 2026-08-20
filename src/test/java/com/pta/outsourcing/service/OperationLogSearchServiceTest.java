package com.pta.outsourcing.service;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.assertj.core.api.Assertions.assertThat;

import com.pta.outsourcing.entity.OperationLog;
import com.pta.outsourcing.enums.OperationResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OperationLogSearchServiceTest {

    @Test
    void shouldSkipIndexWhenDisabledOrMissingId() {
        OperationLogSearchService disabled = new OperationLogSearchService(
                RestClient.builder(), false, "http://localhost:9200", "pta-operation-logs");
        disabled.index(log(null));

        OperationLogSearchService enabled = new OperationLogSearchService(
                RestClient.builder(), true, "http://localhost:9200", "pta-operation-logs");
        enabled.index(log(null));
    }

    @Test
    void shouldSkipSearchWhenDisabledOrInvalidRequest() {
        OperationLogSearchService disabled = new OperationLogSearchService(
                RestClient.builder(), false, "http://localhost:9200", "pta-operation-logs");
        OperationLogSearchService enabled = new OperationLogSearchService(
                RestClient.builder(), true, "http://localhost:9200", "pta-operation-logs");

        assertThat(disabled.searchIds("登录", 10)).isEmpty();
        assertThat(enabled.searchIds(" ", 10)).isEmpty();
        assertThat(enabled.searchIds("登录", 0)).isEmpty();
    }

    @Test
    void shouldIndexOperationLogDocument() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OperationLogSearchService service = new OperationLogSearchService(
                builder, true, "http://localhost:9200", "pta-operation-logs");
        server.expect(once(), requestTo("http://localhost:9200/pta-operation-logs/_doc/7"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess());

        service.index(log(7L));

        server.verify();
    }

    @Test
    void shouldSearchOperationLogIdsAndParseHits() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OperationLogSearchService service = new OperationLogSearchService(
                builder, true, "http://localhost:9200", "pta-operation-logs");
        server.expect(once(), requestTo("http://localhost:9200/pta-operation-logs/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "size": 20,
                          "_source": ["id"],
                          "query": {
                            "multi_match": {
                              "query": "登录",
                              "fields": [
                                "operatorName",
                                "moduleName",
                                "operationType",
                                "requestPath",
                                "requestParams",
                                "errorMessage"
                              ]
                            }
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "hits": {
                            "hits": [
                              {"_source": {"id": 7}},
                              {"_id": "8"},
                              {"_source": {"id": "7"}},
                              {"_id": "bad"}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<Long> ids = service.searchIds("登录", 20);

        assertThat(ids).containsExactly(7L, 8L);
        server.verify();
    }

    @Test
    void shouldClampSearchLimitInsideService() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OperationLogSearchService service = new OperationLogSearchService(
                builder, true, "http://localhost:9200", "pta-operation-logs");
        server.expect(once(), requestTo("http://localhost:9200/pta-operation-logs/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"size\":1000}"))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));

        assertThat(service.searchIds("登录", 2000)).isEmpty();
        server.verify();
    }

    private OperationLog log(Long id) {
        OperationLog log = new OperationLog();
        log.setId(id);
        log.setOperatorId(1L);
        log.setOperatorName("admin");
        log.setModuleName("认证");
        log.setOperationType("登录");
        log.setRequestPath("POST /api/auth/login");
        log.setRequestParams("{\"username\":\"admin\"}");
        log.setResult(OperationResult.SUCCESS.name());
        log.setErrorMessage(null);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}

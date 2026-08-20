package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.vo.OperationLogVO;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.sql.init.mode=never")
@Sql(statements = {
        "DROP TABLE IF EXISTS operation_log",
        "CREATE TABLE operation_log ("
                + "id BIGINT PRIMARY KEY, "
                + "operator_id BIGINT NULL, "
                + "operator_name VARCHAR(64) NULL, "
                + "module_name VARCHAR(64) NOT NULL, "
                + "operation_type VARCHAR(64) NOT NULL, "
                + "request_path VARCHAR(255) NULL, "
                + "request_params VARCHAR(1000) NULL, "
                + "result VARCHAR(32) NOT NULL, "
                + "error_message VARCHAR(1000) NULL, "
                + "created_at TIMESTAMP NOT NULL)"
})
class OperationLogServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OperationLogService operationLogService;

    @MockBean
    private OperationLogSearchService operationLogSearchService;

    @BeforeEach
    void setUp() {
        insertLog(1L, 1L, "admin", "认证", "登录", "POST /api/auth/login",
                "{\"event\":\"mysql-fallback\"}", LocalDateTime.of(2026, 8, 14, 10, 0));
        insertLog(2L, 1L, "admin", "认证", "审批", "POST /api/approvals/2/approve",
                "{\"event\":\"es-only\"}", LocalDateTime.of(2026, 8, 14, 11, 0));
        insertLog(3L, 2L, "leader", "认证", "审批", "POST /api/approvals/3/approve",
                "{\"event\":\"wrong-operator\"}", LocalDateTime.of(2026, 8, 14, 11, 10));
        insertLog(4L, 1L, "admin", "绩效", "审批", "POST /api/performances",
                "{\"event\":\"wrong-module\"}", LocalDateTime.of(2026, 8, 14, 11, 20));
        insertLog(5L, 1L, "admin", "认证", "审批", "POST /api/approvals/5/approve",
                "{\"event\":\"wrong-time\"}", LocalDateTime.of(2026, 8, 14, 13, 0));
        insertLog(6L, 1L, "admin", "认证", "登录", "POST /api/auth/login",
                "{\"event\":\"mysql-latest\"}", LocalDateTime.of(2026, 8, 14, 11, 30));
    }

    @Test
    void shouldMergeElasticsearchCandidatesAndMysqlLikeWithoutBypassingOuterFilters() {
        when(operationLogSearchService.searchIds(eq("登录"), anyInt())).thenReturn(List.of(2L, 3L, 4L, 5L));

        PageVO<OperationLogVO> result = operationLogService.pageLogs(
                1L,
                "认证",
                "登录",
                LocalDateTime.of(2026, 8, 14, 9, 0),
                LocalDateTime.of(2026, 8, 14, 12, 0),
                1,
                10
        );

        assertThat(result.records())
                .extracting(OperationLogVO::id)
                .containsExactly(6L, 2L, 1L);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    void shouldFallbackToMysqlLikeWhenElasticsearchReturnsNoCandidates() {
        when(operationLogSearchService.searchIds(eq("登录"), anyInt())).thenReturn(List.of());

        PageVO<OperationLogVO> result = operationLogService.pageLogs(
                1L,
                "认证",
                "登录",
                LocalDateTime.of(2026, 8, 14, 9, 0),
                LocalDateTime.of(2026, 8, 14, 12, 0),
                1,
                10
        );

        assertThat(result.records())
                .extracting(OperationLogVO::id)
                .containsExactly(6L, 1L);
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void shouldFallbackToMysqlLikeWhenElasticsearchSearchFails() {
        doThrow(new RuntimeException("es down"))
                .when(operationLogSearchService).searchIds(eq("登录"), anyInt());

        PageVO<OperationLogVO> result = operationLogService.pageLogs(
                1L,
                "认证",
                "登录",
                LocalDateTime.of(2026, 8, 14, 9, 0),
                LocalDateTime.of(2026, 8, 14, 12, 0),
                1,
                10
        );

        assertThat(result.records())
                .extracting(OperationLogVO::id)
                .containsExactly(6L, 1L);
        assertThat(result.total()).isEqualTo(2);
    }

    private void insertLog(
            Long id,
            Long operatorId,
            String operatorName,
            String moduleName,
            String operationType,
            String requestPath,
            String requestParams,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO operation_log (
                    id,
                    operator_id,
                    operator_name,
                    module_name,
                    operation_type,
                    request_path,
                    request_params,
                    result,
                    error_message,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'SUCCESS', NULL, ?)
                """,
                id,
                operatorId,
                operatorName,
                moduleName,
                operationType,
                requestPath,
                requestParams,
                Timestamp.valueOf(createdAt)
        );
    }
}

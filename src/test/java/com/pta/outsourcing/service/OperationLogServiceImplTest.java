package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pta.outsourcing.audit.OperationLogSanitizer;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.entity.OperationLog;
import com.pta.outsourcing.enums.OperationResult;
import com.pta.outsourcing.mapper.OperationLogMapper;
import com.pta.outsourcing.service.impl.OperationLogServiceImpl;
import com.pta.outsourcing.vo.OperationLogVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OperationLogServiceImplTest {

    private OperationLogMapper operationLogMapper;
    private OperationLogSearchService operationLogSearchService;
    private OperationLogService operationLogService;

    @BeforeEach
    void setUp() {
        operationLogMapper = mock(OperationLogMapper.class);
        operationLogSearchService = mock(OperationLogSearchService.class);
        operationLogService = new OperationLogServiceImpl(
                operationLogMapper,
                operationLogSearchService,
                new OperationLogSanitizer(new ObjectMapper())
        );
    }

    @Test
    void shouldMaskSensitiveParamsAndIndexLog() {
        when(operationLogMapper.insert(any(OperationLog.class))).thenAnswer(invocation -> {
            OperationLog logEntity = invocation.getArgument(0);
            logEntity.setId(1L);
            return 1;
        });

        operationLogService.record(
                1L,
                "admin",
                "认证",
                "登录",
                "POST /api/auth/login",
                "{\"username\":\"admin\",\"password\":\"Admin@123456\",\"token\":\"raw-token\"}",
                OperationResult.SUCCESS,
                null
        );

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getRequestParams()).contains("\"password\":\"******\"");
        assertThat(captor.getValue().getRequestParams()).contains("\"token\":\"******\"");
        assertThat(captor.getValue().getRequestParams()).doesNotContain("Admin@123456");
        verify(operationLogSearchService).index(captor.getValue());
    }

    @Test
    void shouldExposeMysqlAuditWriteFailureAndSkipElasticsearchIndex() {
        doThrow(new RuntimeException("db down")).when(operationLogMapper).insert(any(OperationLog.class));

        assertThatThrownBy(() -> operationLogService.record(
                1L,
                "admin",
                "认证",
                "登录",
                "POST /api/auth/login",
                "{\"username\":\"admin\"}",
                OperationResult.SUCCESS,
                null
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("权威操作日志写入失败");
        verify(operationLogSearchService, never()).index(any(OperationLog.class));
    }

    @Test
    void shouldKeepMysqlAuditLogWhenElasticsearchIndexFails() {
        when(operationLogMapper.insert(any(OperationLog.class))).thenAnswer(invocation -> {
            OperationLog logEntity = invocation.getArgument(0);
            logEntity.setId(2L);
            return 1;
        });
        doThrow(new RuntimeException("es down")).when(operationLogSearchService).index(any(OperationLog.class));

        assertThatCode(() -> operationLogService.record(
                1L,
                "admin",
                "认证",
                "登录",
                "POST /api/auth/login",
                "{\"username\":\"admin\"}",
                OperationResult.SUCCESS,
                null
        )).doesNotThrowAnyException();

        verify(operationLogMapper).insert(any(OperationLog.class));
        verify(operationLogSearchService).index(any(OperationLog.class));
    }

    @Test
    void shouldUseMysqlAsKeywordSearchAuthorityWhenElasticsearchIndexMissesLogs() {
        OperationLog logEntity = new OperationLog();
        logEntity.setId(5L);
        logEntity.setOperatorName("admin");
        logEntity.setModuleName("认证");
        logEntity.setOperationType("登录");
        logEntity.setResult(OperationResult.SUCCESS.name());
        logEntity.setCreatedAt(LocalDateTime.of(2026, 8, 8, 9, 0));
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(List.of(logEntity));
        page.setTotal(1);
        when(operationLogMapper.selectPage(any(), any())).thenReturn(page);

        PageVO<OperationLogVO> result = operationLogService.pageLogs(
                null,
                null,
                "登录",
                null,
                null,
                1,
                10
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().get(0).operationType()).isEqualTo("登录");
    }

    @Test
    void shouldNormalizePageBoundsBeforeQueryingMysql() {
        Page<OperationLog> page = new Page<>(1, 100);
        page.setRecords(List.of());
        page.setTotal(0);
        when(operationLogMapper.selectPage(any(), any())).thenReturn(page);

        PageVO<OperationLogVO> result = operationLogService.pageLogs(
                null,
                null,
                null,
                null,
                null,
                0,
                101
        );

        ArgumentCaptor<Page<OperationLog>> pageCaptor = ArgumentCaptor.captor();
        verify(operationLogMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
        assertThat(result.pageNo()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(100);
    }
}

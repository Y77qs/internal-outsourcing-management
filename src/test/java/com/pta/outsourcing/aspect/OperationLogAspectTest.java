package com.pta.outsourcing.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.audit.OperationLogSanitizer;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.LoginRequest;
import com.pta.outsourcing.enums.OperationResult;
import com.pta.outsourcing.service.OperationLogService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;

class OperationLogAspectTest {

    private OperationLogService operationLogService;
    private OperationLogAspect operationLogAspect;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        operationLogService = mock(OperationLogService.class);
        operationLogAspect = new OperationLogAspect(
                operationLogService,
                objectMapper,
                new OperationLogSanitizer(objectMapper)
        );
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldMaskLongSensitiveParamsBeforeTruncating() throws Throwable {
        String rawPassword = "P".repeat(1500);
        LoginRequest request = new LoginRequest("admin", rawPassword);
        Method method = TestEndpoint.class.getDeclaredMethod("login", LoginRequest.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, request);

        operationLogAspect.record(joinPoint, operationLog);

        ArgumentCaptor<String> paramsCaptor = ArgumentCaptor.forClass(String.class);
        verify(operationLogService).record(
                any(),
                eq("admin"),
                eq("认证"),
                eq("登录"),
                eq("TestEndpoint.login(..)"),
                paramsCaptor.capture(),
                eq(OperationResult.SUCCESS),
                isNull()
        );
        assertThat(paramsCaptor.getValue()).contains("\"password\":\"******\"");
        assertThat(paramsCaptor.getValue()).doesNotContain(rawPassword.substring(0, 32));
        assertThat(paramsCaptor.getValue()).hasSizeLessThanOrEqualTo(1000);
    }

    @Test
    void shouldPreserveBusinessExceptionWhenFailedAuditWriteAlsoFails() throws Throwable {
        LoginRequest request = new LoginRequest("admin", "Admin@123456");
        Method method = TestEndpoint.class.getDeclaredMethod("login", LoginRequest.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, request);
        RuntimeException businessException = new RuntimeException("业务失败");
        RuntimeException auditException = new IllegalStateException("权威操作日志写入失败");
        when(joinPoint.proceed()).thenThrow(businessException);
        doThrow(auditException).when(operationLogService).record(
                any(),
                eq("admin"),
                eq("认证"),
                eq("登录"),
                eq("TestEndpoint.login(..)"),
                any(),
                eq(OperationResult.FAILED),
                eq("业务失败")
        );

        assertThatThrownBy(() -> operationLogAspect.record(joinPoint, operationLog))
                .isSameAs(businessException)
                .satisfies(throwable -> assertThat(throwable.getSuppressed()).containsExactly(auditException));
    }

    private ProceedingJoinPoint mockJoinPoint(Method method, LoginRequest request) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.toShortString()).thenReturn("TestEndpoint.login(..)");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {request});
        when(joinPoint.proceed()).thenReturn(ResultVO.success("ok"));
        return joinPoint;
    }

    private static class TestEndpoint {

        @OperationLog(moduleName = "认证", operationType = "登录")
        String login(LoginRequest request) {
            return request.username();
        }
    }
}

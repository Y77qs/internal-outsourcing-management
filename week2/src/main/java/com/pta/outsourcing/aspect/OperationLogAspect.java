package com.pta.outsourcing.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.LoginRequest;
import com.pta.outsourcing.dto.RegisterRequest;
import com.pta.outsourcing.enums.OperationResult;
import com.pta.outsourcing.security.CurrentUser;
import com.pta.outsourcing.security.SecurityUtils;
import com.pta.outsourcing.service.OperationLogService;
import com.pta.outsourcing.vo.LoginResponse;
import com.pta.outsourcing.vo.UserVO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 1000;

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /**
     * 围绕标记了操作日志的方法采集成功和失败结果，异常继续交给全局异常处理。
     */
    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Object result = null;
        try {
            result = joinPoint.proceed();
            recordLog(joinPoint, operationLog, result, OperationResult.SUCCESS, null);
            return result;
        } catch (Throwable throwable) {
            recordLog(joinPoint, operationLog, result, OperationResult.FAILED, throwable.getMessage());
            throw throwable;
        }
    }

    private void recordLog(
            ProceedingJoinPoint joinPoint,
            OperationLog operationLog,
            Object result,
            OperationResult operationResult,
            String errorMessage
    ) {
        OperatorInfo operatorInfo = resolveOperator(result, joinPoint.getArgs());
        operationLogService.record(
                operatorInfo.operatorId(),
                operatorInfo.operatorName(),
                operationLog.moduleName(),
                operationLog.operationType(),
                resolveRequestPath(joinPoint),
                resolveRequestParams(joinPoint),
                operationResult,
                errorMessage
        );
    }

    private OperatorInfo resolveOperator(Object result, Object[] args) {
        try {
            CurrentUser currentUser = SecurityUtils.currentUser();
            return new OperatorInfo(currentUser.id(), currentUser.username());
        } catch (RuntimeException exception) {
            // 登录和注册接口执行时还没有认证上下文，需要从响应或请求参数中反推操作人。
            return resolveAnonymousOperator(result, args);
        }
    }

    private OperatorInfo resolveAnonymousOperator(Object result, Object[] args) {
        if (result instanceof ResultVO<?> resultVO) {
            Object data = resultVO.data();
            if (data instanceof LoginResponse loginResponse) {
                return new OperatorInfo(loginResponse.userId(), loginResponse.username());
            }
            if (data instanceof UserVO userVO) {
                return new OperatorInfo(userVO.id(), userVO.username());
            }
        }
        for (Object arg : args) {
            if (arg instanceof LoginRequest loginRequest) {
                return new OperatorInfo(null, loginRequest.username());
            }
            if (arg instanceof RegisterRequest registerRequest) {
                return new OperatorInfo(null, registerRequest.username());
            }
        }
        return new OperatorInfo(null, null);
    }

    private String resolveRequestPath(ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getMethod() + " " + request.getRequestURI();
        }
        return joinPoint.getSignature().toShortString();
    }

    private String resolveRequestParams(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        List<Object> safeArgs = new ArrayList<>();
        for (int index = 0; index < args.length; index++) {
            Object arg = args[index];
            // Servlet 对象和请求头可能包含 Token，不参与序列化，后续服务层还会兜底脱敏。
            if (shouldSkip(arg, parameterAnnotations[index])) {
                continue;
            }
            safeArgs.add(arg);
        }
        try {
            return truncate(objectMapper.writeValueAsString(safeArgs));
        } catch (JsonProcessingException exception) {
            return truncate(safeArgs.toString());
        }
    }

    private boolean shouldSkip(Object arg, Annotation[] annotations) {
        if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
            return true;
        }
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestHeader) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String params) {
        if (params == null || params.length() <= MAX_PARAM_LENGTH) {
            return params;
        }
        return params.substring(0, MAX_PARAM_LENGTH);
    }

    private record OperatorInfo(Long operatorId, String operatorName) {
    }
}

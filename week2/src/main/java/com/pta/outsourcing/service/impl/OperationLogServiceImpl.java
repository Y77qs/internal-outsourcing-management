package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.entity.OperationLog;
import com.pta.outsourcing.enums.OperationResult;
import com.pta.outsourcing.mapper.OperationLogMapper;
import com.pta.outsourcing.service.OperationLogService;
import com.pta.outsourcing.vo.OperationLogVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void record(
            Long operatorId,
            String operatorName,
            String moduleName,
            String operationType,
            String requestPath,
            String requestParams,
            OperationResult result,
            String errorMessage
    ) {
        try {
            OperationLog logEntity = new OperationLog();
            logEntity.setOperatorId(operatorId);
            logEntity.setOperatorName(operatorName);
            logEntity.setModuleName(moduleName);
            logEntity.setOperationType(operationType);
            logEntity.setRequestPath(requestPath);
            logEntity.setRequestParams(maskSensitive(requestParams));
            logEntity.setResult(result.name());
            logEntity.setErrorMessage(truncate(errorMessage));
            operationLogMapper.insert(logEntity);
        } catch (Exception exception) {
            log.warn("Failed to record operation log, module={}, type={}", moduleName, operationType, exception);
        }
    }

    @Override
    public PageVO<OperationLogVO> pageLogs(
            Long operatorId,
            String moduleName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long pageNo,
            long pageSize
    ) {
        IPage<OperationLog> page = operationLogMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<OperationLog>lambdaQuery()
                        .eq(operatorId != null, OperationLog::getOperatorId, operatorId)
                        .like(StringUtils.isNotBlank(moduleName), OperationLog::getModuleName, moduleName)
                        .ge(startTime != null, OperationLog::getCreatedAt, startTime)
                        .le(endTime != null, OperationLog::getCreatedAt, endTime)
                        .orderByDesc(OperationLog::getCreatedAt, OperationLog::getId));
        List<OperationLogVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageVO<>(records, page.getTotal(), pageNo, pageSize);
    }

    private String maskSensitive(String params) {
        if (params == null) {
            return null;
        }
        // 同时处理 JSON、表单样式和 Bearer Token 三类常见敏感参数格式。
        String masked = params.replaceAll("(?i)(\"(?:password|token|authorization)\"\\s*:\\s*\")[^\"]*(\")",
                "$1******$2");
        masked = masked.replaceAll("(?i)((?:password|token|authorization)\\s*=)[^,}&\\s]+", "$1******");
        masked = masked.replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._~+\\-/]+=*", "$1******");
        return truncate(masked);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private OperationLogVO toVO(OperationLog logEntity) {
        return new OperationLogVO(
                logEntity.getId(),
                logEntity.getOperatorId(),
                logEntity.getOperatorName(),
                logEntity.getModuleName(),
                logEntity.getOperationType(),
                logEntity.getRequestPath(),
                logEntity.getRequestParams(),
                logEntity.getResult(),
                logEntity.getErrorMessage(),
                logEntity.getCreatedAt()
        );
    }
}

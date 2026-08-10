package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.audit.OperationLogSanitizer;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.PageQuery;
import com.pta.outsourcing.entity.OperationLog;
import com.pta.outsourcing.enums.OperationResult;
import com.pta.outsourcing.mapper.OperationLogMapper;
import com.pta.outsourcing.service.OperationLogService;
import com.pta.outsourcing.service.OperationLogSearchService;
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
    private final OperationLogSearchService operationLogSearchService;
    private final OperationLogSanitizer operationLogSanitizer;

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
        OperationLog logEntity = new OperationLog();
        logEntity.setOperatorId(operatorId);
        logEntity.setOperatorName(operatorName);
        logEntity.setModuleName(moduleName);
        logEntity.setOperationType(operationType);
        logEntity.setRequestPath(requestPath);
        logEntity.setRequestParams(operationLogSanitizer.sanitize(requestParams));
        logEntity.setResult(result.name());
        logEntity.setErrorMessage(operationLogSanitizer.sanitize(errorMessage));
        try {
            operationLogMapper.insert(logEntity);
        } catch (Exception exception) {
            log.error("Failed to write authoritative operation log to MySQL, module={}, type={}",
                    moduleName, operationType, exception);
            throw new IllegalStateException("权威操作日志写入失败", exception);
        }
        try {
            operationLogSearchService.index(logEntity);
        } catch (Exception exception) {
            log.warn("Skip Elasticsearch operation log indexing, id={}", logEntity.getId(), exception);
        }
    }

    @Override
    public PageVO<OperationLogVO> pageLogs(
            Long operatorId,
            String moduleName,
            String keyword,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long pageNo,
            long pageSize
    ) {
        if (StringUtils.isNotBlank(keyword)) {
            // MySQL 是权威日志库，ES 写入是 best-effort，keyword 查询不能因 ES 索引缺口漏数据。
            return pageLogsFromMysql(operatorId, moduleName, keyword, startTime, endTime, pageNo, pageSize);
        }
        return pageLogsFromMysql(operatorId, moduleName, null, startTime, endTime, pageNo, pageSize);
    }

    private PageVO<OperationLogVO> pageLogsFromMysql(
            Long operatorId,
            String moduleName,
            String keyword,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long pageNo,
            long pageSize
    ) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize);
        IPage<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(pageQuery.pageNo(), pageQuery.pageSize()),
                Wrappers.<OperationLog>lambdaQuery()
                        .eq(operatorId != null, OperationLog::getOperatorId, operatorId)
                        .like(StringUtils.isNotBlank(moduleName), OperationLog::getModuleName, moduleName)
                        .and(StringUtils.isNotBlank(keyword), query -> query
                                .like(OperationLog::getOperatorName, keyword)
                                .or()
                                .like(OperationLog::getModuleName, keyword)
                                .or()
                                .like(OperationLog::getOperationType, keyword)
                                .or()
                                .like(OperationLog::getRequestPath, keyword)
                                .or()
                                .like(OperationLog::getRequestParams, keyword)
                                .or()
                                .like(OperationLog::getErrorMessage, keyword))
                        .ge(startTime != null, OperationLog::getCreatedAt, startTime)
                        .le(endTime != null, OperationLog::getCreatedAt, endTime)
                        .orderByDesc(OperationLog::getCreatedAt, OperationLog::getId));
        List<OperationLogVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageVO<>(records, page.getTotal(), pageQuery.pageNo(), pageQuery.pageSize());
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

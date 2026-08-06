package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.service.OperationLogService;
import com.pta.outsourcing.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 管理员按操作人、模块和时间范围分页查询审计日志。
     *
     * @param operatorId 操作人 ID，可为空。
     * @param moduleName 模块名称，可为空，支持模糊匹配。
     * @param startTime 操作开始时间，可为空。
     * @param endTime 操作结束时间，可为空。
     * @param pageNo 当前页码，从 1 开始。
     * @param pageSize 每页记录数。
     * @return 操作日志分页数据。
     */
    @Operation(summary = "查询操作日志")
    @OperationLog(moduleName = "操作日志", operationType = "查询日志")
    @GetMapping
    @PreAuthorize("hasAuthority('operation:read')")
    public ResultVO<PageVO<OperationLogVO>> pageLogs(
            @Parameter(description = "操作人 ID", example = "1")
            @RequestParam(required = false) Long operatorId,
            @Parameter(description = "模块名称，支持模糊匹配", example = "认证")
            @RequestParam(required = false) String moduleName,
            @Parameter(description = "开始时间，格式：yyyy-MM-dd'T'HH:mm:ss", example = "2026-08-03T09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @Parameter(description = "结束时间，格式：yyyy-MM-dd'T'HH:mm:ss", example = "2026-08-03T18:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(operationLogService.pageLogs(operatorId, moduleName, startTime, endTime,
                pageNo, pageSize));
    }
}

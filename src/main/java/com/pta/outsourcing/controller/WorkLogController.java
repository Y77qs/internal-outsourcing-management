package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.WorkLogCreateRequest;
import com.pta.outsourcing.dto.WorkLogUpdateRequest;
import com.pta.outsourcing.service.WorkLogService;
import com.pta.outsourcing.vo.WorkLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作日志")
@RestController
@RequestMapping("/api/work-logs")
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    /**
     * 测试外包人员提交个人工作日志。
     *
     * @param request 工作日志提交请求。
     * @return 新增后的工作日志。
     */
    @Operation(summary = "提交工作日志")
    @OperationLog(moduleName = "工作日志", operationType = "提交日志")
    @PostMapping
    @PreAuthorize("hasAuthority('worklog:create')")
    public ResultVO<WorkLogVO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "工作日志提交请求", required = true)
            @Valid @RequestBody WorkLogCreateRequest request
    ) {
        return ResultVO.success(workLogService.create(request));
    }

    /**
     * 修改当前登录用户自己的工作日志。
     *
     * @param workLogId 工作日志 ID。
     * @param request 工作日志修改请求。
     * @return 修改后的工作日志。
     */
    @Operation(summary = "修改个人工作日志")
    @OperationLog(moduleName = "工作日志", operationType = "修改日志")
    @PutMapping("/{workLogId}")
    @PreAuthorize("hasAuthority('worklog:update:self')")
    public ResultVO<WorkLogVO> update(
            @Parameter(description = "工作日志 ID", example = "1")
            @PathVariable Long workLogId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "工作日志修改请求", required = true)
            @Valid @RequestBody WorkLogUpdateRequest request
    ) {
        return ResultVO.success(workLogService.update(workLogId, request));
    }

    /**
     * 查询当前登录用户的工作日志。
     *
     * @param projectId 项目 ID，可为空。
     * @param startDate 开始日期，可为空。
     * @param endDate 结束日期，可为空。
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 个人工作日志分页数据。
     */
    @Operation(summary = "查询个人工作日志")
    @OperationLog(moduleName = "工作日志", operationType = "查询个人日志")
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('worklog:read:self')")
    public ResultVO<PageVO<WorkLogVO>> pageMine(
            @Parameter(description = "项目 ID", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "开始日期", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-08-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(workLogService.pageMine(startDate, endDate, projectId, pageNo, pageSize));
    }

    /**
     * 领导或管理员按人员、项目和日期查询全部工作日志。
     *
     * @param userId 提交人 ID，可为空。
     * @param projectId 项目 ID，可为空。
     * @param startDate 开始日期，可为空。
     * @param endDate 结束日期，可为空。
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 全部工作日志分页数据。
     */
    @Operation(summary = "查询全部工作日志")
    @OperationLog(moduleName = "工作日志", operationType = "查询全部日志")
    @GetMapping
    @PreAuthorize("hasAuthority('worklog:read:all')")
    public ResultVO<PageVO<WorkLogVO>> pageAll(
            @Parameter(description = "提交人 ID", example = "3")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "项目 ID", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "开始日期", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-08-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(workLogService.pageAll(userId, projectId, startDate, endDate, pageNo, pageSize));
    }
}

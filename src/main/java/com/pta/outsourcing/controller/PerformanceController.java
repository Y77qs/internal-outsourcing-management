package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.PerformanceCreateRequest;
import com.pta.outsourcing.dto.PerformanceUpdateRequest;
import com.pta.outsourcing.service.PerformanceService;
import com.pta.outsourcing.vo.PerformanceRecordVO;
import com.pta.outsourcing.vo.PerformanceUserOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "绩效管理")
@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    /**
     * 领导或管理员新增绩效记录。
     *
     * @param request 绩效新增请求。
     * @return 新增后的当前有效绩效。
     */
    @Operation(summary = "新增绩效记录")
    @OperationLog(moduleName = "绩效管理", operationType = "新增绩效")
    @PostMapping
    @PreAuthorize("hasAuthority('performance:write')")
    public ResultVO<PerformanceRecordVO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "绩效新增请求", required = true)
            @Valid @RequestBody PerformanceCreateRequest request
    ) {
        return ResultVO.success(performanceService.create(request));
    }

    /**
     * 领导或管理员修改当前有效绩效，系统保留历史版本。
     *
     * @param performanceId 绩效记录 ID。
     * @param request 绩效修改请求。
     * @return 修改后生成的新当前绩效。
     */
    @Operation(summary = "修改绩效记录")
    @OperationLog(moduleName = "绩效管理", operationType = "修改绩效")
    @PutMapping("/{performanceId}")
    @PreAuthorize("hasAuthority('performance:write')")
    public ResultVO<PerformanceRecordVO> update(
            @Parameter(description = "绩效记录 ID", example = "1")
            @PathVariable Long performanceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "绩效修改请求", required = true)
            @Valid @RequestBody PerformanceUpdateRequest request
    ) {
        return ResultVO.success(performanceService.update(performanceId, request));
    }

    /**
     * 领导或管理员分页查询绩效记录。
     *
     * @param evaluatedUserId 被评价用户 ID，可为空。
     * @param evaluatedUserIds 被评价用户 ID 集合，可为空。
     * @param projectId 项目 ID，可为空。
     * @param periodType 周期类型，可为空。
     * @param periodValue 周期值，可为空。
     * @param current 是否只看当前有效记录，可为空。
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 绩效分页数据。
     */
    @Operation(summary = "查询绩效列表")
    @OperationLog(moduleName = "绩效管理", operationType = "查询绩效")
    @GetMapping
    @PreAuthorize("hasAuthority('performance:read')")
    public ResultVO<PageVO<PerformanceRecordVO>> pageRecords(
            @Parameter(description = "被评价用户 ID", example = "3")
            @RequestParam(required = false) Long evaluatedUserId,
            @Parameter(description = "被评价用户 ID 集合", example = "3")
            @RequestParam(required = false) List<Long> evaluatedUserIds,
            @Parameter(description = "项目 ID", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "周期类型：MONTH、QUARTER、PROJECT", example = "MONTH")
            @RequestParam(required = false) String periodType,
            @Parameter(description = "周期值", example = "2026-08")
            @RequestParam(required = false) String periodValue,
            @Parameter(description = "是否当前有效记录", example = "true")
            @RequestParam(required = false) Boolean current,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(performanceService.pageRecords(evaluatedUserId, evaluatedUserIds, projectId,
                periodType, periodValue, current, pageNo, pageSize));
    }

    /**
     * 按姓名或 ID 搜索绩效人员选项。
     *
     * @param name 真实姓名模糊查询条件，可为空。
     * @param userId 用户 ID 精确查询条件，可为空。
     * @return 最多 20 条人员选项。
     */
    @Operation(summary = "搜索绩效人员选项")
    @OperationLog(moduleName = "绩效管理", operationType = "搜索绩效人员")
    @GetMapping("/user-options")
    @PreAuthorize("hasAnyAuthority('performance:read','performance:write')")
    public ResultVO<List<PerformanceUserOptionVO>> searchUserOptions(
            @Parameter(description = "真实姓名模糊查询条件", example = "张三")
            @RequestParam(required = false) String name,
            @Parameter(description = "用户 ID", example = "3")
            @RequestParam(required = false) Long userId
    ) {
        return ResultVO.success(performanceService.searchUserOptions(name, userId));
    }

    /**
     * 测试外包人员查询自己的绩效记录。
     *
     * @param projectId 项目 ID，可为空。
     * @param current 是否只看当前有效记录，可为空。
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 个人绩效分页数据。
     */
    @Operation(summary = "查询个人绩效")
    @OperationLog(moduleName = "绩效管理", operationType = "查询个人绩效")
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('performance:read:self')")
    public ResultVO<PageVO<PerformanceRecordVO>> pageMine(
            @Parameter(description = "项目 ID", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "是否当前有效记录", example = "true")
            @RequestParam(required = false) Boolean current,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(performanceService.pageMine(projectId, current, pageNo, pageSize));
    }

    /**
     * 领导或管理员查询指定人员绩效历史。
     *
     * @param evaluatedUserId 被评价用户 ID。
     * @param projectId 项目 ID，可为空。
     * @param periodType 周期类型，可为空。
     * @param periodValue 周期值，可为空。
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 绩效历史分页数据。
     */
    @Operation(summary = "查询绩效历史")
    @OperationLog(moduleName = "绩效管理", operationType = "查询绩效历史")
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('performance:read')")
    public ResultVO<PageVO<PerformanceRecordVO>> history(
            @Parameter(description = "被评价用户 ID", example = "3")
            @RequestParam Long evaluatedUserId,
            @Parameter(description = "项目 ID", example = "1")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "周期类型：MONTH、QUARTER、PROJECT", example = "MONTH")
            @RequestParam(required = false) String periodType,
            @Parameter(description = "周期值", example = "2026-08")
            @RequestParam(required = false) String periodValue,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(performanceService.history(evaluatedUserId, projectId, periodType, periodValue,
                pageNo, pageSize));
    }

    /**
     * 领导或管理员查询绩效详情。
     *
     * @param performanceId 绩效记录 ID。
     * @return 绩效详情。
     */
    @Operation(summary = "查询绩效详情")
    @OperationLog(moduleName = "绩效管理", operationType = "查询绩效详情")
    @GetMapping("/{performanceId}")
    @PreAuthorize("hasAuthority('performance:read')")
    public ResultVO<PerformanceRecordVO> detail(
            @Parameter(description = "绩效记录 ID", example = "1")
            @PathVariable Long performanceId
    ) {
        return ResultVO.success(performanceService.detail(performanceId));
    }
}

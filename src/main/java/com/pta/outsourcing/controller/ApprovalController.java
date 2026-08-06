package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.ApprovalBatchRequest;
import com.pta.outsourcing.dto.ApprovalRequest;
import com.pta.outsourcing.service.ApprovalService;
import com.pta.outsourcing.vo.ApplicationVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "领导审批")
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 分页查询待审批上岗申请。
     *
     * @param pageNo 当前页码，从 1 开始。
     * @param pageSize 每页记录数。
     * @return 待审批申请分页列表。
     */
    @Operation(summary = "分页查询待审批申请")
    @OperationLog(moduleName = "领导审批", operationType = "查询待审批")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('approval:read')")
    public ResultVO<PageVO<ApplicationVO>> pagePending(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(approvalService.pagePending(pageNo, pageSize));
    }

    /**
     * 审批通过指定上岗申请。
     *
     * @param applicationId 上岗申请 ID。
     * @param request 审批意见请求体，审批通过时意见可为空。
     * @return 审批通过后的申请详情。
     */
    @Operation(summary = "审批通过")
    @OperationLog(moduleName = "领导审批", operationType = "审批通过")
    @PostMapping("/{applicationId}/approve")
    @PreAuthorize("hasAuthority('approval:write')")
    public ResultVO<ApplicationVO> approve(
            @Parameter(description = "待审批的上岗申请 ID", example = "1")
            @PathVariable Long applicationId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "审批通过意见请求", required = true)
            @RequestBody ApprovalRequest request
    ) {
        return ResultVO.success(approvalService.approve(applicationId, request));
    }

    /**
     * 驳回指定上岗申请，审批意见必填。
     *
     * @param applicationId 上岗申请 ID。
     * @param request 审批意见请求体，驳回时 `opinion` 必填。
     * @return 驳回后的申请详情。
     */
    @Operation(summary = "驳回申请")
    @OperationLog(moduleName = "领导审批", operationType = "驳回申请")
    @PostMapping("/{applicationId}/reject")
    @PreAuthorize("hasAuthority('approval:write')")
    public ResultVO<ApplicationVO> reject(
            @Parameter(description = "待驳回的上岗申请 ID", example = "1")
            @PathVariable Long applicationId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "驳回审批意见请求", required = true)
            @RequestBody ApprovalRequest request
    ) {
        return ResultVO.success(approvalService.reject(applicationId, request));
    }

    /**
     * 批量处理申请；前端全选时传入当前页选中的申请 ID。
     *
     * @param request 批量审批请求体，包含当前页选中的申请 ID、审批结果和审批意见。
     * @return 批量处理后的申请详情列表。
     */
    @Operation(summary = "批量审批或驳回")
    @OperationLog(moduleName = "领导审批", operationType = "批量处理")
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('approval:write')")
    public ResultVO<List<ApplicationVO>> batch(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "批量审批或批量驳回请求", required = true)
            @Valid @RequestBody ApprovalBatchRequest request
    ) {
        return ResultVO.success(approvalService.batchProcess(request));
    }
}

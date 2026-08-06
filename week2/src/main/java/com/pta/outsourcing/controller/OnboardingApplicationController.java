package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.OnboardingApplicationCreateRequest;
import com.pta.outsourcing.service.OnboardingApplicationService;
import com.pta.outsourcing.vo.ApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "上岗申请")
@RestController
@RequestMapping("/api/onboarding/applications")
@RequiredArgsConstructor
public class OnboardingApplicationController {

    private final OnboardingApplicationService onboardingApplicationService;

    /**
     * 测试外包人员提交上岗申请。
     *
     * @param request 上岗申请请求体，包含部门、项目、岗位类型和申请原因。
     * @return 新创建的上岗申请详情。
     */
    @Operation(summary = "提交上岗申请")
    @OperationLog(moduleName = "上岗申请", operationType = "提交申请")
    @PostMapping
    @PreAuthorize("hasAuthority('application:create')")
    public ResultVO<ApplicationVO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "测试外包人员上岗申请请求", required = true)
            @Valid @RequestBody OnboardingApplicationCreateRequest request
    ) {
        return ResultVO.success(onboardingApplicationService.create(request));
    }

    /**
     * 查询当前用户自己的上岗申请列表。
     *
     * @param pageNo 当前页码，从 1 开始。
     * @param pageSize 每页记录数。
     * @return 当前登录用户的上岗申请分页数据。
     */
    @Operation(summary = "查询个人申请列表")
    @OperationLog(moduleName = "上岗申请", operationType = "查询个人申请")
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('application:read:self')")
    public ResultVO<PageVO<ApplicationVO>> pageMine(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(onboardingApplicationService.pageMine(pageNo, pageSize));
    }

    /**
     * 查询申请详情；申请人、领导或管理员可访问。
     *
     * @param applicationId 上岗申请 ID。
     * @return 指定上岗申请详情，包含审批结果和审批意见。
     */
    @Operation(summary = "查询申请详情")
    @OperationLog(moduleName = "上岗申请", operationType = "查询申请详情")
    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyAuthority('application:read:self','approval:read','user:read')")
    public ResultVO<ApplicationVO> detail(
            @Parameter(description = "上岗申请 ID", example = "1")
            @PathVariable Long applicationId
    ) {
        return ResultVO.success(onboardingApplicationService.detail(applicationId));
    }

    /**
     * 撤回仍处于待审批状态的个人申请。
     *
     * @param applicationId 上岗申请 ID。
     * @return 撤回后的上岗申请详情。
     */
    @Operation(summary = "撤回待审批申请")
    @OperationLog(moduleName = "上岗申请", operationType = "撤回申请")
    @PostMapping("/{applicationId}/withdraw")
    @PreAuthorize("hasAuthority('application:withdraw')")
    public ResultVO<ApplicationVO> withdraw(
            @Parameter(description = "待撤回的上岗申请 ID", example = "1")
            @PathVariable Long applicationId
    ) {
        return ResultVO.success(onboardingApplicationService.withdraw(applicationId));
    }
}

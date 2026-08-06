package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.UserCreateRequest;
import com.pta.outsourcing.dto.UserRoleUpdateRequest;
import com.pta.outsourcing.dto.UserStatusUpdateRequest;
import com.pta.outsourcing.service.UserService;
import com.pta.outsourcing.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 管理员创建内部账号。
     *
     * <p>用于创建领导、管理员或其他内部账号。公开注册仍只面向测试外包人员，内部账号必须由管理员
     * 在用户管理模块指定角色后创建。</p>
     *
     * @param request 用户创建请求，包含初始密码、基础资料和角色 ID 集合。
     * @return 创建后的用户详情。
     */
    @Operation(summary = "管理员创建用户")
    @OperationLog(moduleName = "用户管理", operationType = "创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ResultVO<UserVO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "管理员创建用户请求", required = true)
            @Valid @RequestBody UserCreateRequest request
    ) {
        return ResultVO.success(userService.create(request));
    }

    /**
     * 管理员分页查询用户。
     *
     * @param username 用户名模糊查询条件，可为空。
     * @param status 用户状态筛选，可选值为 ENABLED 或 DISABLED。
     * @param pageNo 当前页码，从 1 开始。
     * @param pageSize 每页记录数。
     * @return 分页用户列表，包含角色和权限信息。
     */
    @Operation(summary = "查询用户列表")
    @OperationLog(moduleName = "用户管理", operationType = "查询用户列表")
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResultVO<PageVO<UserVO>> pageUsers(
            @Parameter(description = "用户名模糊查询条件", example = "tester")
            @RequestParam(required = false) String username,
            @Parameter(description = "用户状态：ENABLED 或 DISABLED", example = "ENABLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(userService.pageUsers(username, status, pageNo, pageSize));
    }

    /**
     * 管理员查询用户详情。
     *
     * @param userId 用户 ID。
     * @return 用户详情、角色编码和权限编码。
     */
    @Operation(summary = "查询用户详情")
    @OperationLog(moduleName = "用户管理", operationType = "查询用户详情")
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResultVO<UserVO> detail(
            @Parameter(description = "用户 ID", example = "3")
            @PathVariable Long userId
    ) {
        return ResultVO.success(userService.detail(userId));
    }

    /**
     * 管理员启用或禁用用户。
     *
     * @param userId 用户 ID。
     * @param request 状态更新请求体，状态只能是 ENABLED 或 DISABLED。
     * @return 更新后的用户详情。
     */
    @Operation(summary = "启用或禁用用户")
    @OperationLog(moduleName = "用户管理", operationType = "更新用户状态")
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('user:write')")
    public ResultVO<UserVO> updateStatus(
            @Parameter(description = "用户 ID", example = "3")
            @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户启用或禁用请求", required = true)
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ResultVO.success(userService.updateStatus(userId, request));
    }

    /**
     * 管理员为用户分配角色。
     *
     * @param userId 用户 ID。
     * @param request 角色分配请求体，包含角色 ID 集合。
     * @return 更新角色后的用户详情。
     */
    @Operation(summary = "为用户分配角色")
    @OperationLog(moduleName = "用户管理", operationType = "分配用户角色")
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('user:write')")
    public ResultVO<UserVO> updateRoles(
            @Parameter(description = "用户 ID", example = "3")
            @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户角色分配请求", required = true)
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        return ResultVO.success(userService.updateRoles(userId, request));
    }
}

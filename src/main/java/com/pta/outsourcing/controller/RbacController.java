package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.service.RbacService;
import com.pta.outsourcing.vo.PermissionVO;
import com.pta.outsourcing.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "角色权限")
@RestController
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    /**
     * 查询系统角色列表。
     *
     * @return 系统角色列表，包含角色编码、名称、描述和状态。
     */
    @Operation(summary = "查询角色列表")
    @OperationLog(moduleName = "权限管理", operationType = "查询角色")
    @GetMapping("/api/roles")
    @PreAuthorize("hasAuthority('role:read')")
    public ResultVO<List<RoleVO>> roles() {
        return ResultVO.success(rbacService.listRoles());
    }

    /**
     * 查询系统权限列表。
     *
     * @return 系统权限列表，包含权限编码、模块、接口路径和请求方法。
     */
    @Operation(summary = "查询权限列表")
    @OperationLog(moduleName = "权限管理", operationType = "查询权限")
    @GetMapping("/api/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ResultVO<List<PermissionVO>> permissions() {
        return ResultVO.success(rbacService.listPermissions());
    }
}

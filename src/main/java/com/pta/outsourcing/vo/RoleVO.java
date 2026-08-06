package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "角色响应")
public record RoleVO(
        @Schema(description = "角色 ID", example = "1")
        Long id,
        @Schema(description = "角色编码", example = "ADMIN")
        String roleCode,
        @Schema(description = "角色名称", example = "系统管理员")
        String roleName,
        @Schema(description = "角色描述", example = "拥有系统管理、用户权限和审计日志查询权限")
        String description,
        @Schema(description = "角色状态：ENABLED 或 DISABLED", example = "ENABLED")
        String status
) {
}

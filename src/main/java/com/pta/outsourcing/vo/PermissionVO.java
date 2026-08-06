package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "权限响应")
public record PermissionVO(
        @Schema(description = "权限 ID", example = "1")
        Long id,
        @Schema(description = "权限编码", example = "user:read")
        String permissionCode,
        @Schema(description = "权限名称", example = "查询用户")
        String permissionName,
        @Schema(description = "所属模块", example = "用户管理")
        String moduleName,
        @Schema(description = "权限类型", example = "API")
        String permissionType,
        @Schema(description = "接口路径", example = "/api/users/**")
        String apiPath,
        @Schema(description = "HTTP 请求方法", example = "GET")
        String httpMethod
) {
}

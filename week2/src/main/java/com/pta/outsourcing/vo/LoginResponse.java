package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(description = "登录响应")
public record LoginResponse(
        @Schema(description = "Token 类型", example = "Bearer")
        String tokenType,
        @Schema(description = "JWT Token，后续请求放入 Authorization 请求头")
        String token,
        @Schema(description = "Token 有效期，单位秒", example = "7200")
        long expiresIn,
        @Schema(description = "用户 ID", example = "1")
        Long userId,
        @Schema(description = "用户名", example = "admin")
        String username,
        @Schema(description = "角色编码集合", example = "[\"ADMIN\"]")
        Set<String> roles,
        @Schema(description = "权限编码集合", example = "[\"user:read\",\"operation:read\"]")
        Set<String> permissions
) {
}

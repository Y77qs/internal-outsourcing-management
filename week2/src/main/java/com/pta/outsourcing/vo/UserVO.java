package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "用户信息响应")
public record UserVO(
        @Schema(description = "用户 ID", example = "3")
        Long id,
        @Schema(description = "用户名", example = "tester01")
        String username,
        @Schema(description = "手机号", example = "13800000002")
        String phone,
        @Schema(description = "邮箱", example = "tester01@example.com")
        String email,
        @Schema(description = "真实姓名", example = "测试外包一号")
        String realName,
        @Schema(description = "所属部门 ID", example = "2")
        Long departmentId,
        @Schema(description = "用户状态：ENABLED 或 DISABLED", example = "ENABLED")
        String status,
        @Schema(description = "角色编码集合", example = "[\"OUTSOURCER\"]")
        Set<String> roles,
        @Schema(description = "权限编码集合", example = "[\"application:create\",\"application:read:self\"]")
        Set<String> permissions,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}

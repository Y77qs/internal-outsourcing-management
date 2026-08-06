package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "管理员创建内部账号请求")
public record UserCreateRequest(
        @Schema(description = "用户名，系统内唯一", example = "leader02")
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名不能超过 64 个字符")
        String username,

        @Schema(description = "初始密码，创建时加密存储", example = "Leader@123456")
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度必须为 8 到 32 位")
        String password,

        @Schema(description = "手机号", example = "13800000003")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "邮箱", example = "leader02@example.com")
        @Email(message = "邮箱格式不正确")
        String email,

        @Schema(description = "真实姓名", example = "审批领导二号")
        @Size(max = 64, message = "真实姓名不能超过 64 个字符")
        String realName,

        @Schema(description = "所属部门 ID，可为空", example = "2")
        Long departmentId,

        @Schema(description = "账号状态，可选 ENABLED 或 DISABLED；为空时默认 ENABLED", example = "ENABLED")
        @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "用户状态只能是 ENABLED 或 DISABLED")
        String status,

        @Schema(description = "角色 ID 集合，必须来自系统已有角色", example = "[2]")
        @NotEmpty(message = "角色 ID 集合不能为空")
        Set<Long> roleIds
) {
}

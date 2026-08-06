package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "注册请求")
public record RegisterRequest(
        @Schema(description = "用户名，唯一", example = "tester01")
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名不能超过 64 个字符")
        String username,

        @Schema(description = "密码", example = "Tester@123456")
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度必须为 8 到 32 位")
        String password,

        @Schema(description = "手机号", example = "13800000002")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "邮箱", example = "tester01@example.com")
        @Email(message = "邮箱格式不正确")
        String email,

        @Schema(description = "真实姓名", example = "测试外包一号")
        @Size(max = 64, message = "真实姓名不能超过 64 个字符")
        String realName
) {
}

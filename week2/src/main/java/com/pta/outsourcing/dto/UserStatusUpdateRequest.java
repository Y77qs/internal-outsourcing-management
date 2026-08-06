package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户状态更新请求")
public record UserStatusUpdateRequest(
        @Schema(description = "用户状态：ENABLED 或 DISABLED", example = "DISABLED")
        @NotBlank(message = "用户状态不能为空")
        String status
) {
}

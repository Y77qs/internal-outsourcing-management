package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

@Schema(description = "用户角色分配请求")
public record UserRoleUpdateRequest(
        @Schema(description = "角色 ID 集合", example = "[2,3]")
        @NotEmpty(message = "角色 ID 集合不能为空")
        Set<Long> roleIds
) {
}

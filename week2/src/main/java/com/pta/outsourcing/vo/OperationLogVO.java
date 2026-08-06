package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "操作日志响应")
public record OperationLogVO(
        @Schema(description = "操作日志 ID", example = "1")
        Long id,
        @Schema(description = "操作人用户 ID", example = "1")
        Long operatorId,
        @Schema(description = "操作人用户名", example = "admin")
        String operatorName,
        @Schema(description = "业务模块名称", example = "认证")
        String moduleName,
        @Schema(description = "操作类型", example = "用户登录")
        String operationType,
        @Schema(description = "请求路径", example = "POST /api/auth/login")
        String requestPath,
        @Schema(description = "请求参数，密码和 Token 已脱敏")
        String requestParams,
        @Schema(description = "操作结果：SUCCESS 或 FAILED", example = "SUCCESS")
        String result,
        @Schema(description = "失败原因，成功时为空")
        String errorMessage,
        @Schema(description = "操作时间")
        LocalDateTime createdAt
) {
}

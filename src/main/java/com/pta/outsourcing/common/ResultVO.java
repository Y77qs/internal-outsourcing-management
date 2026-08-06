package com.pta.outsourcing.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.slf4j.MDC;

@Schema(description = "统一响应体")
public record ResultVO<T>(
        @Schema(description = "业务状态码，00000 表示成功", example = "00000")
        String code,
        @Schema(description = "响应消息", example = "成功")
        String message,
        @Schema(description = "业务数据，失败时为空")
        T data,
        @Schema(description = "链路追踪 ID，可为空", example = "trace-001")
        String traceId,
        @Schema(description = "响应时间")
        LocalDateTime timestamp
) {

    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data,
                MDC.get("traceId"),
                LocalDateTime.now()
        );
    }

    public static ResultVO<Void> success() {
        return success(null);
    }

    public static ResultVO<Void> fail(ErrorCode errorCode, String message) {
        return new ResultVO<>(errorCode.getCode(), message, null, MDC.get("traceId"), LocalDateTime.now());
    }
}

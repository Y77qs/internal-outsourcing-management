package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "新增绩效记录请求")
public record PerformanceCreateRequest(
        @Schema(description = "被评价用户 ID", example = "3")
        @NotNull(message = "被评价用户不能为空")
        Long evaluatedUserId,

        @Schema(description = "项目 ID", example = "1")
        @NotNull(message = "项目不能为空")
        Long projectId,

        @Schema(description = "绩效周期类型：MONTH、QUARTER、PROJECT", example = "MONTH")
        @NotBlank(message = "绩效周期类型不能为空")
        String periodType,

        @Schema(description = "绩效周期值，MONTH 使用 yyyy-MM，QUARTER 使用 yyyy-Qn，PROJECT 可为空",
                example = "2026-08")
        @Size(max = 32, message = "绩效周期值不能超过 32 个字符")
        String periodValue,

        @Schema(description = "绩效等级：A、B、C", example = "A")
        @NotBlank(message = "绩效等级不能为空")
        String grade,

        @Schema(description = "评价说明", example = "按时完成测试任务，问题反馈及时")
        @Size(max = 1000, message = "评价说明不能超过 1000 个字符")
        String comment
) {
}

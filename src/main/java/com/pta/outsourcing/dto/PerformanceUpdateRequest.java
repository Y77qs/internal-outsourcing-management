package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "修改绩效记录请求")
public record PerformanceUpdateRequest(
        @Schema(description = "绩效等级：A、B、C", example = "B")
        @NotBlank(message = "绩效等级不能为空")
        String grade,

        @Schema(description = "评价说明", example = "补充回归测试质量反馈")
        @Size(max = 1000, message = "评价说明不能超过 1000 个字符")
        String comment,

        @Schema(description = "修改原因", example = "根据补充验收结果调整绩效")
        @NotBlank(message = "修改绩效时必须填写修改原因")
        @Size(max = 1000, message = "修改原因不能超过 1000 个字符")
        String modificationReason
) {
}

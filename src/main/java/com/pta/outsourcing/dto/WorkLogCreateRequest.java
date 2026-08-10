package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "提交工作日志请求")
public record WorkLogCreateRequest(
        @Schema(description = "项目 ID", example = "1")
        @NotNull(message = "项目不能为空")
        Long projectId,

        @Schema(description = "工作日期", example = "2026-08-08")
        @NotNull(message = "工作日期不能为空")
        LocalDate workDate,

        @Schema(description = "工作内容", example = "完成上岗申请和审批流程回归测试")
        @NotBlank(message = "工作内容不能为空")
        @Size(max = 2000, message = "工作内容不能超过 2000 个字符")
        String workContent,

        @Schema(description = "问题记录", example = "审批意见为空时需要前端提示")
        @Size(max = 1000, message = "问题记录不能超过 1000 个字符")
        String issueRecord,

        @Schema(description = "完成情况", example = "已完成主要场景验证")
        @NotBlank(message = "完成情况不能为空")
        @Size(max = 1000, message = "完成情况不能超过 1000 个字符")
        String completionStatus
) {
}

package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "工作日志响应")
public record WorkLogVO(
        @Schema(description = "工作日志 ID", example = "1")
        Long id,
        @Schema(description = "提交人用户 ID", example = "3")
        Long userId,
        @Schema(description = "提交人用户名", example = "tester01")
        String username,
        @Schema(description = "提交人真实姓名", example = "测试外包一号")
        String realName,
        @Schema(description = "项目 ID", example = "1")
        Long projectId,
        @Schema(description = "项目名称", example = "内部测试外包人员管理系统")
        String projectName,
        @Schema(description = "工作日期", example = "2026-08-08")
        LocalDate workDate,
        @Schema(description = "工作内容")
        String workContent,
        @Schema(description = "问题记录")
        String issueRecord,
        @Schema(description = "完成情况")
        String completionStatus,
        @Schema(description = "提交时间")
        LocalDateTime submittedAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}

package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "绩效记录响应")
public record PerformanceRecordVO(
        @Schema(description = "绩效记录 ID", example = "1")
        Long id,
        @Schema(description = "评价人用户 ID", example = "2")
        Long evaluatorUserId,
        @Schema(description = "评价人用户名", example = "leader")
        String evaluatorName,
        @Schema(description = "被评价用户 ID", example = "3")
        Long evaluatedUserId,
        @Schema(description = "被评价用户名", example = "tester01")
        String evaluatedUsername,
        @Schema(description = "被评价人真实姓名", example = "测试外包一号")
        String evaluatedRealName,
        @Schema(description = "项目 ID", example = "1")
        Long projectId,
        @Schema(description = "项目名称", example = "内部测试外包人员管理系统")
        String projectName,
        @Schema(description = "绩效周期类型：MONTH、QUARTER、PROJECT", example = "MONTH")
        String periodType,
        @Schema(description = "绩效周期值", example = "2026-08")
        String periodValue,
        @Schema(description = "绩效等级：A、B、C", example = "A")
        String grade,
        @Schema(description = "评价说明")
        String comment,
        @Schema(description = "是否当前有效记录", example = "true")
        Boolean current,
        @Schema(description = "修改原因")
        String modificationReason,
        @Schema(description = "生效时间")
        LocalDateTime effectiveAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}

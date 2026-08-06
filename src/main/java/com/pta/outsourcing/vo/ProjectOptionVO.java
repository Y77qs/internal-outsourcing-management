package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "项目下拉选项")
public record ProjectOptionVO(
        @Schema(description = "项目 ID", example = "1")
        Long id,

        @Schema(description = "所属部门 ID", example = "2")
        Long departmentId,

        @Schema(description = "项目编码", example = "PTA-OUTSOURCING")
        String projectCode,

        @Schema(description = "项目名称", example = "内部测试外包人员管理系统")
        String projectName,

        @Schema(description = "项目开始日期", example = "2026-08-03")
        LocalDate startDate,

        @Schema(description = "项目结束日期", example = "2026-08-07")
        LocalDate endDate,

        @Schema(description = "项目状态", example = "ENABLED")
        String status
) {
}

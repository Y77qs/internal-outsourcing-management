package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "部门下拉选项")
public record DepartmentOptionVO(
        @Schema(description = "部门 ID", example = "2")
        Long id,

        @Schema(description = "部门编码", example = "TEST_PLATFORM")
        String departmentCode,

        @Schema(description = "部门名称", example = "测试平台部")
        String departmentName,

        @Schema(description = "父级部门 ID", example = "1")
        Long parentId,

        @Schema(description = "部门状态", example = "ENABLED")
        String status
) {
}

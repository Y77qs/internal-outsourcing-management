package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "绩效人员搜索选项")
public record PerformanceUserOptionVO(
        @Schema(description = "用户 ID", example = "3")
        Long id,
        @Schema(description = "用户名", example = "tester01")
        String username,
        @Schema(description = "真实姓名", example = "张三")
        String realName,
        @Schema(description = "用户状态", example = "ENABLED")
        String status
) {
}

package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "提交上岗申请请求")
public record OnboardingApplicationCreateRequest(
        @Schema(description = "所属部门 ID", example = "2")
        @NotNull(message = "所属部门不能为空")
        Long departmentId,

        @Schema(description = "项目 ID", example = "1")
        @NotNull(message = "项目不能为空")
        Long projectId,

        @Schema(description = "岗位类型", example = "功能测试")
        @NotBlank(message = "岗位类型不能为空")
        @Size(max = 64, message = "岗位类型不能超过 64 个字符")
        String positionType,

        @Schema(description = "申请原因", example = "参与内部测试外包人员管理系统功能测试")
        @NotBlank(message = "申请原因不能为空")
        @Size(max = 1000, message = "申请原因不能超过 1000 个字符")
        String applicationReason
) {
}

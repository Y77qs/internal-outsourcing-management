package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "上岗申请响应")
public record ApplicationVO(
        @Schema(description = "上岗申请 ID", example = "1")
        Long id,
        @Schema(description = "申请人用户 ID", example = "3")
        Long applicantId,
        @Schema(description = "申请人用户名", example = "tester01")
        String applicantName,
        @Schema(description = "部门 ID", example = "2")
        Long departmentId,
        @Schema(description = "部门名称", example = "测试平台部")
        String departmentName,
        @Schema(description = "项目 ID", example = "1")
        Long projectId,
        @Schema(description = "项目名称", example = "内部测试外包人员管理系统")
        String projectName,
        @Schema(description = "岗位类型", example = "功能测试")
        String positionType,
        @Schema(description = "申请原因", example = "参与内部测试外包人员管理系统功能测试")
        String applicationReason,
        @Schema(description = "申请状态：PENDING、APPROVED、REJECTED、WITHDRAWN", example = "PENDING")
        String status,
        @Schema(description = "审批结果：APPROVED 或 REJECTED，未审批时为空", example = "APPROVED")
        String approvalResult,
        @Schema(description = "审批意见", example = "资料完整，同意上岗")
        String approvalOpinion,
        @Schema(description = "审批人用户名", example = "leader")
        String approverName,
        @Schema(description = "审批时间")
        LocalDateTime approvedAt,
        @Schema(description = "提交时间")
        LocalDateTime submittedAt,
        @Schema(description = "撤回时间，未撤回时为空")
        LocalDateTime withdrawnAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}

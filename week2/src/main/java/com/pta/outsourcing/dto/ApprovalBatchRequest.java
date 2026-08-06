package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "批量审批请求，全选时传入当前页选中的申请 ID")
public record ApprovalBatchRequest(
        @Schema(description = "申请 ID 列表", example = "[1,2,3]")
        @NotEmpty(message = "申请 ID 列表不能为空")
        List<Long> applicationIds,

        @Schema(description = "审批结果：APPROVED 或 REJECTED", example = "APPROVED")
        @NotNull(message = "审批结果不能为空")
        String result,

        @Schema(description = "审批意见；驳回时必填", example = "资料完整，同意上岗")
        String opinion
) {
}

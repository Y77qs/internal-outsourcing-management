package com.pta.outsourcing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审批请求")
public record ApprovalRequest(
        @Schema(description = "审批意见；驳回时必填", example = "申请原因不充分，请补充项目说明")
        String opinion
) {
}

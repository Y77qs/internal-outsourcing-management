package com.pta.outsourcing.mq;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "上岗申请通知 MQ 事件")
public record OnboardingNotificationEvent(
        @Schema(description = "事件唯一 ID")
        String eventId,
        @Schema(description = "上岗申请 ID", example = "1")
        Long applicationId,
        @Schema(description = "申请人用户 ID", example = "3")
        Long applicantId,
        @Schema(description = "通知接收人用户 ID", example = "2")
        Long recipientId,
        @Schema(description = "通知类型", example = "APPLICATION_SUBMITTED")
        String type,
        @Schema(description = "通知标题", example = "新的上岗申请待审批")
        String title,
        @Schema(description = "通知内容", example = "用户 tester01 提交了上岗申请，请及时处理。")
        String content,
        @Schema(description = "事件创建时间")
        LocalDateTime createdAt
) {
}

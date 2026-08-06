package com.pta.outsourcing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "通知消息响应")
public record NotificationMessageVO(
        @Schema(description = "通知消息 ID", example = "1")
        Long id,
        @Schema(description = "通知事件唯一 ID")
        String eventId,
        @Schema(description = "关联的上岗申请 ID", example = "1")
        Long applicationId,
        @Schema(description = "接收人用户 ID", example = "3")
        Long recipientId,
        @Schema(description = "事件类型", example = "APPLICATION_APPROVED")
        String eventType,
        @Schema(description = "通知标题", example = "上岗申请已通过")
        String title,
        @Schema(description = "通知内容", example = "你的上岗申请已通过。")
        String content,
        @Schema(description = "通知状态：PENDING、SENT、FAILED", example = "SENT")
        String status,
        @Schema(description = "重试次数", example = "0")
        Integer retryCount,
        @Schema(description = "失败原因，成功时为空")
        String errorMessage,
        @Schema(description = "发送成功时间")
        LocalDateTime sentAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}

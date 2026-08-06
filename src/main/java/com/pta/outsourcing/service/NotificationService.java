package com.pta.outsourcing.service;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.enums.NotificationType;
import com.pta.outsourcing.mq.OnboardingNotificationEvent;
import com.pta.outsourcing.vo.NotificationMessageVO;

public interface NotificationService {

    /**
     * 创建上岗申请通知记录并发布 MQ 事件。
     *
     * @param applicationId 上岗申请 ID。
     * @param applicantId 申请人用户 ID。
     * @param recipientId 通知接收人用户 ID。
     * @param type 通知类型。
     * @param title 通知标题。
     * @param content 通知内容。
     */
    void publishOnboardingEvent(
            Long applicationId,
            Long applicantId,
            Long recipientId,
            NotificationType type,
            String title,
            String content
    );

    /**
     * MQ 消费成功后标记通知为已发送。
     *
     * @param event 通知 MQ 事件。
     */
    void markSent(OnboardingNotificationEvent event);

    /**
     * MQ 发送或消费失败后标记通知为失败。
     *
     * @param event 通知 MQ 事件。
     * @param reason 失败原因。
     */
    void markFailed(OnboardingNotificationEvent event, String reason);

    /**
     * 分页查询当前用户可见通知。
     *
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 通知消息分页数据。
     */
    PageVO<NotificationMessageVO> pageMine(long pageNo, long pageSize);
}

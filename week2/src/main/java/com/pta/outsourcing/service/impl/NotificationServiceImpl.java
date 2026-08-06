package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.config.RabbitConfig;
import com.pta.outsourcing.entity.NotificationMessage;
import com.pta.outsourcing.enums.NotificationStatus;
import com.pta.outsourcing.enums.NotificationType;
import com.pta.outsourcing.mapper.NotificationMessageMapper;
import com.pta.outsourcing.mq.OnboardingNotificationEvent;
import com.pta.outsourcing.security.SecurityUtils;
import com.pta.outsourcing.service.NotificationService;
import com.pta.outsourcing.vo.NotificationMessageVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMessageMapper notificationMessageMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOnboardingEvent(
            Long applicationId,
            Long applicantId,
            Long recipientId,
            NotificationType type,
            String title,
            String content
    ) {
        String eventId = UUID.randomUUID().toString();
        NotificationMessage message = new NotificationMessage();
        message.setEventId(eventId);
        message.setApplicationId(applicationId);
        message.setRecipientId(recipientId);
        message.setEventType(type.name());
        message.setTitle(title);
        message.setContent(content);
        message.setStatus(NotificationStatus.PENDING.name());
        message.setRetryCount(0);
        notificationMessageMapper.insert(message);

        // 通知先落库再投递 MQ，消费者根据 eventId 回写状态，保证消息处理过程可追踪。
        OnboardingNotificationEvent event = new OnboardingNotificationEvent(
                eventId,
                applicationId,
                applicantId,
                recipientId,
                type.name(),
                title,
                content,
                LocalDateTime.now()
        );
        // 如果当前在审批或申请事务中，必须等事务提交后再发 MQ，避免消费者先于数据库提交读不到通知。
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEvent(event);
                }
            });
        } else {
            sendEvent(event);
        }
    }

    @Override
    public void markSent(OnboardingNotificationEvent event) {
        NotificationMessage message = findByEventId(event.eventId());
        if (message == null) {
            return;
        }
        message.setStatus(NotificationStatus.SENT.name());
        message.setSentAt(LocalDateTime.now());
        message.setErrorMessage(null);
        notificationMessageMapper.updateById(message);
    }

    @Override
    public void markFailed(OnboardingNotificationEvent event, String reason) {
        NotificationMessage message = findByEventId(event.eventId());
        if (message == null) {
            return;
        }
        message.setStatus(NotificationStatus.FAILED.name());
        message.setRetryCount(message.getRetryCount() == null ? 1 : message.getRetryCount() + 1);
        message.setErrorMessage(reason == null ? "消息消费失败" : truncate(reason));
        notificationMessageMapper.updateById(message);
    }

    @Override
    public PageVO<NotificationMessageVO> pageMine(long pageNo, long pageSize) {
        var currentUser = SecurityUtils.currentUser();
        boolean canReadAll = SecurityUtils.hasPermission("approval:read") || SecurityUtils.hasPermission("user:read");
        IPage<NotificationMessage> page = notificationMessageMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<NotificationMessage>lambdaQuery()
                        .eq(!canReadAll, NotificationMessage::getRecipientId, currentUser.id())
                        .orderByDesc(NotificationMessage::getCreatedAt));
        List<NotificationMessageVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(records, page.getTotal(), pageNo, pageSize);
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void handleOnboardingNotification(OnboardingNotificationEvent event) {
        log.info("Consume onboarding notification, eventId={}, type={}, recipientId={}",
                event.eventId(), event.type(), event.recipientId());
        markSent(event);
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_DLQ)
    public void handleDeadLetter(OnboardingNotificationEvent event) {
        log.warn("Onboarding notification entered DLQ, eventId={}, type={}", event.eventId(), event.type());
        // RabbitMQ 重试耗尽后进入死信队列，这里统一把通知状态标记为失败供人工排查。
        markFailed(event, "消息重试后仍消费失败，已进入死信队列");
    }

    private NotificationMessage findByEventId(String eventId) {
        return notificationMessageMapper.selectOne(Wrappers.<NotificationMessage>lambdaQuery()
                .eq(NotificationMessage::getEventId, eventId));
    }

    private void sendEvent(OnboardingNotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.NOTIFICATION_EXCHANGE,
                    RabbitConfig.NOTIFICATION_ROUTING_KEY,
                    event
            );
            log.info("Published onboarding notification event, eventId={}, type={}", event.eventId(), event.type());
        } catch (Exception exception) {
            markFailed(event, exception.getMessage());
        }
    }

    private NotificationMessageVO toVO(NotificationMessage message) {
        return new NotificationMessageVO(
                message.getId(),
                message.getEventId(),
                message.getApplicationId(),
                message.getRecipientId(),
                message.getEventType(),
                message.getTitle(),
                message.getContent(),
                message.getStatus(),
                message.getRetryCount(),
                message.getErrorMessage(),
                message.getSentAt(),
                message.getCreatedAt()
        );
    }

    private String truncate(String message) {
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}

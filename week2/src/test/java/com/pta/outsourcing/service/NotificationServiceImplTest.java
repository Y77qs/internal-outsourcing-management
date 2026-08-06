package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.entity.NotificationMessage;
import com.pta.outsourcing.enums.NotificationStatus;
import com.pta.outsourcing.enums.NotificationType;
import com.pta.outsourcing.mapper.NotificationMessageMapper;
import com.pta.outsourcing.mq.OnboardingNotificationEvent;
import com.pta.outsourcing.service.impl.NotificationServiceImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class NotificationServiceImplTest {

    private NotificationMessageMapper notificationMessageMapper;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationMessageMapper = mock(NotificationMessageMapper.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        notificationService = new NotificationServiceImpl(notificationMessageMapper, rabbitTemplate);
    }

    @Test
    void shouldPersistAndSendNotificationEvent() {
        notificationService.publishOnboardingEvent(
                1L,
                3L,
                2L,
                NotificationType.APPLICATION_SUBMITTED,
                "新的上岗申请待审批",
                "请及时处理"
        );

        verify(notificationMessageMapper).insert(any(NotificationMessage.class));
    }

    @Test
    void shouldMarkMessageSent() {
        NotificationMessage message = new NotificationMessage();
        message.setId(10L);
        message.setEventId("event-1");
        message.setStatus(NotificationStatus.PENDING.name());
        when(notificationMessageMapper.selectOne(any())).thenReturn(message);

        notificationService.markSent(new OnboardingNotificationEvent(
                "event-1",
                1L,
                3L,
                3L,
                NotificationType.APPLICATION_APPROVED.name(),
                "上岗申请已通过",
                "你的上岗申请已通过",
                LocalDateTime.now()
        ));

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationMessageMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT.name());
    }
}

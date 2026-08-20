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
import com.pta.outsourcing.security.CurrentUser;
import com.pta.outsourcing.service.impl.NotificationServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class NotificationServiceImplTest {

    private NotificationMessageMapper notificationMessageMapper;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationMessageMapper = mock(NotificationMessageMapper.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        notificationService = new NotificationServiceImpl(notificationMessageMapper, rabbitTemplate);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    @Test
    void shouldMarkMessageFailedAndTruncateLongReason() {
        NotificationMessage message = new NotificationMessage();
        message.setId(10L);
        message.setEventId("event-1");
        message.setRetryCount(null);
        when(notificationMessageMapper.selectOne(any())).thenReturn(message);

        notificationService.markFailed(event("event-1"), "x".repeat(1200));

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationMessageMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED.name());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        assertThat(captor.getValue().getErrorMessage()).hasSize(1000);
    }

    @Test
    void shouldIgnoreMissingMessageAndHandleListenerMethods() {
        when(notificationMessageMapper.selectOne(any())).thenReturn(null);

        notificationService.markSent(event("missing"));
        notificationService.markFailed(event("missing"), null);
        ((NotificationServiceImpl) notificationService).handleOnboardingNotification(event("missing"));
        ((NotificationServiceImpl) notificationService).handleDeadLetter(event("missing"));
    }

    @Test
    void shouldPageOnlyCurrentUserNotificationsUnlessUserCanReadAll() {
        authenticate(3L, "tester", "notification:read");
        NotificationMessage message = message("event-1", 3L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<NotificationMessage> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(message));
        page.setTotal(1);
        when(notificationMessageMapper.selectPage(any(), any())).thenReturn(page);

        assertThat(notificationService.pageMine(1, 10).records()).singleElement()
                .satisfies(vo -> assertThat(vo.eventId()).isEqualTo("event-1"));

        authenticate(2L, "leader", "notification:read", "approval:read");
        assertThat(notificationService.pageMine(1, 10).total()).isEqualTo(1);
    }

    private OnboardingNotificationEvent event(String eventId) {
        return new OnboardingNotificationEvent(
                eventId,
                1L,
                3L,
                3L,
                NotificationType.APPLICATION_APPROVED.name(),
                "上岗申请已通过",
                "你的上岗申请已通过",
                LocalDateTime.now()
        );
    }

    private NotificationMessage message(String eventId, Long recipientId) {
        NotificationMessage message = new NotificationMessage();
        message.setId(1L);
        message.setEventId(eventId);
        message.setApplicationId(1L);
        message.setRecipientId(recipientId);
        message.setEventType(NotificationType.APPLICATION_APPROVED.name());
        message.setTitle("上岗申请已通过");
        message.setContent("你的上岗申请已通过");
        message.setStatus(NotificationStatus.SENT.name());
        message.setRetryCount(0);
        message.setSentAt(LocalDateTime.now());
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private void authenticate(Long userId, String username, String... permissions) {
        Set<String> permissionSet = Set.of(permissions);
        CurrentUser currentUser = new CurrentUser(userId, username, Set.of("OUTSOURCER"), permissionSet);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUser,
                "token",
                permissionSet.stream().map(SimpleGrantedAuthority::new).toList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

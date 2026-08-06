package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.dto.ApprovalRequest;
import com.pta.outsourcing.entity.ApprovalRecord;
import com.pta.outsourcing.entity.OnboardingApplication;
import com.pta.outsourcing.enums.ApplicationStatus;
import com.pta.outsourcing.mapper.ApprovalRecordMapper;
import com.pta.outsourcing.mapper.OnboardingApplicationMapper;
import com.pta.outsourcing.security.CurrentUser;
import com.pta.outsourcing.service.impl.ApplicationAssembler;
import com.pta.outsourcing.service.impl.ApprovalServiceImpl;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ApprovalServiceImplTest {

    private OnboardingApplicationMapper applicationMapper;
    private ApprovalRecordMapper approvalRecordMapper;
    private NotificationService notificationService;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        applicationMapper = mock(OnboardingApplicationMapper.class);
        approvalRecordMapper = mock(ApprovalRecordMapper.class);
        ApplicationAssembler applicationAssembler = mock(ApplicationAssembler.class);
        notificationService = mock(NotificationService.class);
        approvalService = new ApprovalServiceImpl(
                applicationMapper,
                approvalRecordMapper,
                applicationAssembler,
                notificationService
        );
        CurrentUser currentUser = new CurrentUser(2L, "leader", Set.of("LEADER"), Set.of("approval:write"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                "token",
                Set.of(new SimpleGrantedAuthority("approval:write"))
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectNonPendingApplication() {
        OnboardingApplication application = new OnboardingApplication();
        application.setId(1L);
        application.setStatus(ApplicationStatus.APPROVED.name());
        when(applicationMapper.selectById(1L)).thenReturn(application);

        assertThatThrownBy(() -> approvalService.approve(1L, new ApprovalRequest("同意")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只有待审批申请可以被审批");
    }

    @Test
    void shouldRequireRejectOpinion() {
        assertThatThrownBy(() -> approvalService.reject(1L, new ApprovalRequest("")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("驳回申请必须填写审批意见");
    }

    @Test
    void shouldApprovePendingApplicationAndPublishNotification() {
        OnboardingApplication application = new OnboardingApplication();
        application.setId(1L);
        application.setApplicantId(3L);
        application.setStatus(ApplicationStatus.PENDING.name());
        when(applicationMapper.selectById(1L)).thenReturn(application);

        approvalService.approve(1L, new ApprovalRequest("同意"));

        verify(applicationMapper).updateById(any(OnboardingApplication.class));
        verify(approvalRecordMapper).insert(any(ApprovalRecord.class));
        verify(notificationService).publishOnboardingEvent(any(), any(), any(), any(), any(), any());
    }
}

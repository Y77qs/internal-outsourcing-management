package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.OnboardingApplicationCreateRequest;
import com.pta.outsourcing.entity.OnboardingApplication;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysDepartment;
import com.pta.outsourcing.enums.ApplicationStatus;
import com.pta.outsourcing.enums.NotificationType;
import com.pta.outsourcing.mapper.OnboardingApplicationMapper;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.security.CurrentUser;
import com.pta.outsourcing.service.impl.ApplicationAssembler;
import com.pta.outsourcing.service.impl.OnboardingApplicationServiceImpl;
import com.pta.outsourcing.vo.ApplicationVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class OnboardingApplicationServiceImplTest {

    private OnboardingApplicationMapper applicationMapper;
    private SysDepartmentMapper departmentMapper;
    private ProjectMapper projectMapper;
    private ApplicationAssembler applicationAssembler;
    private RbacService rbacService;
    private NotificationService notificationService;
    private OnboardingApplicationService service;

    @BeforeEach
    void setUp() {
        applicationMapper = mock(OnboardingApplicationMapper.class);
        departmentMapper = mock(SysDepartmentMapper.class);
        projectMapper = mock(ProjectMapper.class);
        applicationAssembler = mock(ApplicationAssembler.class);
        rbacService = mock(RbacService.class);
        notificationService = mock(NotificationService.class);
        service = new OnboardingApplicationServiceImpl(
                applicationMapper,
                departmentMapper,
                projectMapper,
                applicationAssembler,
                rbacService,
                notificationService
        );
        authenticate(3L, "tester", "application:create", "application:read:self", "application:withdraw");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreatePendingApplicationAndNotifyLeader() {
        OnboardingApplicationCreateRequest request =
                new OnboardingApplicationCreateRequest(2L, 1L, "功能测试", "参与测试");
        ApplicationVO expected = applicationVO(10L);
        when(departmentMapper.selectById(2L)).thenReturn(new SysDepartment());
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(applicationMapper.selectCount(any())).thenReturn(0L);
        when(rbacService.findFirstEnabledUserIdByRoleCode(RbacService.LEADER_ROLE)).thenReturn(2L);
        doAnswer(invocation -> {
            OnboardingApplication application = invocation.getArgument(0);
            application.setId(10L);
            return 1;
        }).when(applicationMapper).insert(any(OnboardingApplication.class));
        when(applicationMapper.selectById(10L)).thenAnswer(invocation -> {
            OnboardingApplication application = new OnboardingApplication();
            application.setId(10L);
            application.setApplicantId(3L);
            application.setDepartmentId(2L);
            application.setProjectId(1L);
            application.setStatus(ApplicationStatus.PENDING.name());
            return application;
        });
        when(applicationAssembler.toVO(any())).thenReturn(expected);

        ApplicationVO actual = service.create(request);

        assertThat(actual).isSameAs(expected);
        verify(notificationService).publishOnboardingEvent(
                10L,
                3L,
                2L,
                NotificationType.APPLICATION_SUBMITTED,
                "新的上岗申请待审批",
                "用户 tester 提交了上岗申请，请及时处理。"
        );
    }

    @Test
    void shouldRejectMissingDepartmentOrProjectAndDuplicatePendingApplication() {
        OnboardingApplicationCreateRequest request =
                new OnboardingApplicationCreateRequest(2L, 1L, "功能测试", "参与测试");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("所属部门不存在");

        when(departmentMapper.selectById(2L)).thenReturn(new SysDepartment());
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("项目不存在");

        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(applicationMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能重复提交");
    }

    @Test
    void shouldPageMineAndAssembleRecords() {
        OnboardingApplication application = application(10L, 3L, ApplicationStatus.PENDING);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OnboardingApplication> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(application));
        page.setTotal(1);
        ApplicationVO vo = applicationVO(10L);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);
        when(applicationAssembler.toVO(application)).thenReturn(vo);

        PageVO<ApplicationVO> result = service.pageMine(1, 10);

        assertThat(result.records()).containsExactly(vo);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void shouldAllowOwnerOrAuditorToViewDetailAndRejectOtherUsers() {
        OnboardingApplication application = application(10L, 3L, ApplicationStatus.PENDING);
        ApplicationVO vo = applicationVO(10L);
        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(applicationAssembler.toVO(application)).thenReturn(vo);

        assertThat(service.detail(10L)).isSameAs(vo);

        authenticate(99L, "leader", "approval:read");
        assertThat(service.detail(10L)).isSameAs(vo);

        authenticate(99L, "other", "application:read:self");
        assertThatThrownBy(() -> service.detail(10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能查看自己的申请");
    }

    @Test
    void shouldWithdrawOwnPendingApplicationAndNotifyLeader() {
        OnboardingApplication application = application(10L, 3L, ApplicationStatus.PENDING);
        ApplicationVO vo = applicationVO(10L);
        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(rbacService.findFirstEnabledUserIdByRoleCode(RbacService.LEADER_ROLE)).thenReturn(2L);
        when(applicationAssembler.toVO(application)).thenReturn(vo);

        assertThat(service.withdraw(10L)).isSameAs(vo);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN.name());
        assertThat(application.getWithdrawnAt()).isNotNull();
        verify(applicationMapper).updateById(application);
        verify(notificationService).publishOnboardingEvent(
                10L,
                3L,
                2L,
                NotificationType.APPLICATION_WITHDRAWN,
                "上岗申请已撤回",
                "用户 tester 撤回了上岗申请。"
        );
    }

    @Test
    void shouldRejectInvalidWithdrawCasesAndMissingApplication() {
        when(applicationMapper.selectById(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.withdraw(404L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("上岗申请不存在");

        when(applicationMapper.selectById(10L)).thenReturn(application(10L, 99L, ApplicationStatus.PENDING));
        assertThatThrownBy(() -> service.withdraw(10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能撤回自己的申请");

        when(applicationMapper.selectById(11L)).thenReturn(application(11L, 3L, ApplicationStatus.APPROVED));
        assertThatThrownBy(() -> service.withdraw(11L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只有待审批申请可以撤回");
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

    private OnboardingApplication application(Long id, Long applicantId, ApplicationStatus status) {
        OnboardingApplication application = new OnboardingApplication();
        application.setId(id);
        application.setApplicantId(applicantId);
        application.setDepartmentId(2L);
        application.setProjectId(1L);
        application.setPositionType("功能测试");
        application.setApplicationReason("参与测试");
        application.setStatus(status.name());
        application.setSubmittedAt(LocalDateTime.now());
        return application;
    }

    private ApplicationVO applicationVO(Long id) {
        return new ApplicationVO(id, 3L, "tester", 2L, "测试部", 1L, "PTA",
                "功能测试", "参与测试", ApplicationStatus.PENDING.name(), null, null, null,
                null, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now());
    }
}

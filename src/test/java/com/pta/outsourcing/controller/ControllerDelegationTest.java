package com.pta.outsourcing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.ApprovalBatchRequest;
import com.pta.outsourcing.dto.ApprovalRequest;
import com.pta.outsourcing.dto.LoginRequest;
import com.pta.outsourcing.dto.OnboardingApplicationCreateRequest;
import com.pta.outsourcing.dto.PerformanceCreateRequest;
import com.pta.outsourcing.dto.PerformanceUpdateRequest;
import com.pta.outsourcing.dto.RegisterRequest;
import com.pta.outsourcing.dto.UserCreateRequest;
import com.pta.outsourcing.dto.UserRoleUpdateRequest;
import com.pta.outsourcing.dto.UserStatusUpdateRequest;
import com.pta.outsourcing.dto.WorkLogCreateRequest;
import com.pta.outsourcing.dto.WorkLogUpdateRequest;
import com.pta.outsourcing.enums.UserStatus;
import com.pta.outsourcing.service.ApprovalService;
import com.pta.outsourcing.service.AuthService;
import com.pta.outsourcing.service.BasicDataService;
import com.pta.outsourcing.service.NotificationService;
import com.pta.outsourcing.service.OnboardingApplicationService;
import com.pta.outsourcing.service.OperationLogService;
import com.pta.outsourcing.service.PerformanceService;
import com.pta.outsourcing.service.RbacService;
import com.pta.outsourcing.service.UserService;
import com.pta.outsourcing.service.WorkLogService;
import com.pta.outsourcing.vo.ApplicationVO;
import com.pta.outsourcing.vo.DepartmentOptionVO;
import com.pta.outsourcing.vo.LoginResponse;
import com.pta.outsourcing.vo.NotificationMessageVO;
import com.pta.outsourcing.vo.OperationLogVO;
import com.pta.outsourcing.vo.PerformanceRecordVO;
import com.pta.outsourcing.vo.PerformanceUserOptionVO;
import com.pta.outsourcing.vo.PermissionVO;
import com.pta.outsourcing.vo.ProjectOptionVO;
import com.pta.outsourcing.vo.RoleVO;
import com.pta.outsourcing.vo.UserVO;
import com.pta.outsourcing.vo.WorkLogVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ControllerDelegationTest {

    @Test
    void authControllerShouldDelegateAllActions() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        RegisterRequest registerRequest = new RegisterRequest(
                "tester", "Tester@123456", "13800000001", "tester@example.com", "测试");
        LoginRequest loginRequest = new LoginRequest("tester", "Tester@123456");
        UserVO user = userVO(3L);
        LoginResponse loginResponse = new LoginResponse("Bearer", "token", 7200, 3L,
                "tester", Set.of("OUTSOURCER"), Set.of("application:create"));
        when(authService.register(registerRequest)).thenReturn(user);
        when(authService.login(loginRequest)).thenReturn(loginResponse);
        when(authService.currentUser()).thenReturn(user);

        assertThat(controller.register(registerRequest).data()).isSameAs(user);
        assertThat(controller.login(loginRequest).data()).isSameAs(loginResponse);
        assertThat(controller.currentUser().data()).isSameAs(user);
        assertThat(controller.logout("Bearer token").code()).isEqualTo("00000");

        verify(authService).logout("Bearer token");
    }

    @Test
    void userControllerShouldDelegateAllActions() {
        UserService userService = mock(UserService.class);
        UserController controller = new UserController(userService);
        UserCreateRequest createRequest = new UserCreateRequest(
                "leader02", "Leader@123456", "13800000002", "leader@example.com",
                "领导二号", 2L, UserStatus.ENABLED.name(), Set.of(2L));
        UserStatusUpdateRequest statusRequest = new UserStatusUpdateRequest(UserStatus.DISABLED.name());
        UserRoleUpdateRequest roleRequest = new UserRoleUpdateRequest(Set.of(2L));
        PageVO<UserVO> page = new PageVO<>(List.of(userVO(10L)), 1, 1, 10);
        UserVO user = userVO(10L);
        when(userService.create(createRequest)).thenReturn(user);
        when(userService.pageUsers("leader", "ENABLED", 1, 10)).thenReturn(page);
        when(userService.detail(10L)).thenReturn(user);
        when(userService.updateStatus(10L, statusRequest)).thenReturn(user);
        when(userService.updateRoles(10L, roleRequest)).thenReturn(user);

        assertThat(controller.create(createRequest).data()).isSameAs(user);
        assertThat(controller.pageUsers("leader", "ENABLED", 1, 10).data()).isSameAs(page);
        assertThat(controller.detail(10L).data()).isSameAs(user);
        assertThat(controller.updateStatus(10L, statusRequest).data()).isSameAs(user);
        assertThat(controller.updateRoles(10L, roleRequest).data()).isSameAs(user);
    }

    @Test
    void onboardingAndApprovalControllersShouldDelegate() {
        OnboardingApplicationService applicationService = mock(OnboardingApplicationService.class);
        ApprovalService approvalService = mock(ApprovalService.class);
        OnboardingApplicationController applicationController =
                new OnboardingApplicationController(applicationService);
        ApprovalController approvalController = new ApprovalController(approvalService);
        OnboardingApplicationCreateRequest createRequest =
                new OnboardingApplicationCreateRequest(2L, 1L, "功能测试", "申请原因");
        ApprovalRequest approvalRequest = new ApprovalRequest("同意");
        ApprovalBatchRequest batchRequest = new ApprovalBatchRequest(List.of(1L), "APPROVED", "同意");
        ApplicationVO application = applicationVO(1L);
        PageVO<ApplicationVO> page = new PageVO<>(List.of(application), 1, 1, 10);
        when(applicationService.create(createRequest)).thenReturn(application);
        when(applicationService.pageMine(1, 10)).thenReturn(page);
        when(applicationService.detail(1L)).thenReturn(application);
        when(applicationService.withdraw(1L)).thenReturn(application);
        when(approvalService.pagePending(1, 10)).thenReturn(page);
        when(approvalService.approve(1L, approvalRequest)).thenReturn(application);
        when(approvalService.reject(1L, approvalRequest)).thenReturn(application);
        when(approvalService.batchProcess(batchRequest)).thenReturn(List.of(application));

        assertThat(applicationController.create(createRequest).data()).isSameAs(application);
        assertThat(applicationController.pageMine(1, 10).data()).isSameAs(page);
        assertThat(applicationController.detail(1L).data()).isSameAs(application);
        assertThat(applicationController.withdraw(1L).data()).isSameAs(application);
        assertThat(approvalController.pagePending(1, 10).data()).isSameAs(page);
        assertThat(approvalController.approve(1L, approvalRequest).data()).isSameAs(application);
        assertThat(approvalController.reject(1L, approvalRequest).data()).isSameAs(application);
        assertThat(approvalController.batch(batchRequest).data()).containsExactly(application);
    }

    @Test
    void workLogAndPerformanceControllersShouldDelegate() {
        WorkLogService workLogService = mock(WorkLogService.class);
        PerformanceService performanceService = mock(PerformanceService.class);
        WorkLogController workLogController = new WorkLogController(workLogService);
        PerformanceController performanceController = new PerformanceController(performanceService);
        WorkLogCreateRequest workCreate = new WorkLogCreateRequest(
                1L, LocalDate.of(2026, 8, 14), "内容", "无", "完成");
        WorkLogUpdateRequest workUpdate = new WorkLogUpdateRequest(
                1L, LocalDate.of(2026, 8, 14), "更新", "无", "完成");
        PerformanceCreateRequest performanceCreate = new PerformanceCreateRequest(
                3L, 1L, "MONTH", "2026-08", "A", "表现稳定");
        PerformanceUpdateRequest performanceUpdate = new PerformanceUpdateRequest(
                "B", "补充说明", "验收调整");
        WorkLogVO workLog = workLogVO();
        PerformanceRecordVO performance = performanceVO();
        PageVO<WorkLogVO> workPage = new PageVO<>(List.of(workLog), 1, 1, 10);
        PageVO<PerformanceRecordVO> performancePage = new PageVO<>(List.of(performance), 1, 1, 10);
        List<PerformanceUserOptionVO> options = List.of(new PerformanceUserOptionVO(
                3L, "tester", "张三", UserStatus.ENABLED.name()));
        when(workLogService.create(workCreate)).thenReturn(workLog);
        when(workLogService.update(1L, workUpdate)).thenReturn(workLog);
        when(workLogService.pageMine(null, null, 1L, 1, 10)).thenReturn(workPage);
        when(workLogService.pageAll(3L, 1L, null, null, 1, 10)).thenReturn(workPage);
        when(performanceService.create(performanceCreate)).thenReturn(performance);
        when(performanceService.update(1L, performanceUpdate)).thenReturn(performance);
        when(performanceService.pageRecords(3L, List.of(3L), 1L, "MONTH", "2026-08", true, 1, 10))
                .thenReturn(performancePage);
        when(performanceService.searchUserOptions("张", 3L)).thenReturn(options);
        when(performanceService.pageMine(1L, true, 1, 10)).thenReturn(performancePage);
        when(performanceService.history(3L, 1L, "MONTH", "2026-08", 1, 10)).thenReturn(performancePage);
        when(performanceService.detail(1L)).thenReturn(performance);

        assertThat(workLogController.create(workCreate).data()).isSameAs(workLog);
        assertThat(workLogController.update(1L, workUpdate).data()).isSameAs(workLog);
        assertThat(workLogController.pageMine(1L, null, null, 1, 10).data()).isSameAs(workPage);
        assertThat(workLogController.pageAll(3L, 1L, null, null, 1, 10).data()).isSameAs(workPage);
        assertThat(performanceController.create(performanceCreate).data()).isSameAs(performance);
        assertThat(performanceController.update(1L, performanceUpdate).data()).isSameAs(performance);
        assertThat(performanceController.pageRecords(3L, List.of(3L), 1L, "MONTH", "2026-08", true, 1, 10)
                .data()).isSameAs(performancePage);
        assertThat(performanceController.searchUserOptions("张", 3L).data()).isSameAs(options);
        assertThat(performanceController.pageMine(1L, true, 1, 10).data()).isSameAs(performancePage);
        assertThat(performanceController.history(3L, 1L, "MONTH", "2026-08", 1, 10).data())
                .isSameAs(performancePage);
        assertThat(performanceController.detail(1L).data()).isSameAs(performance);
    }

    @Test
    void basicRbacNotificationOperationAndUiControllersShouldDelegate() {
        BasicDataService basicDataService = mock(BasicDataService.class);
        RbacService rbacService = mock(RbacService.class);
        NotificationService notificationService = mock(NotificationService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        BasicDataController basicDataController = new BasicDataController(basicDataService);
        RbacController rbacController = new RbacController(rbacService);
        NotificationController notificationController = new NotificationController(notificationService);
        OperationLogController operationLogController = new OperationLogController(operationLogService);
        HealthController healthController = new HealthController();
        UiController uiController = new UiController();
        List<DepartmentOptionVO> departments = List.of(new DepartmentOptionVO(
                2L, "TEST", "测试部", 1L, UserStatus.ENABLED.name()));
        List<ProjectOptionVO> projects = List.of(new ProjectOptionVO(
                1L, 2L, "PTA", "PTA", LocalDate.now(), LocalDate.now(), UserStatus.ENABLED.name()));
        List<RoleVO> roles = List.of(new RoleVO(1L, "ADMIN", "管理员", "系统管理员", "ENABLED"));
        List<PermissionVO> permissions = List.of(new PermissionVO(
                1L, "user:read", "查询用户", "用户管理", "API", "/api/users", "GET"));
        PageVO<NotificationMessageVO> notificationPage = new PageVO<>(List.of(notificationVO()), 1, 1, 10);
        PageVO<OperationLogVO> logPage = new PageVO<>(List.of(operationLogVO()), 1, 1, 10);
        LocalDateTime start = LocalDateTime.of(2026, 8, 14, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 14, 10, 0);
        when(basicDataService.listDepartments()).thenReturn(departments);
        when(basicDataService.listProjects(2L)).thenReturn(projects);
        when(rbacService.listRoles()).thenReturn(roles);
        when(rbacService.listPermissions()).thenReturn(permissions);
        when(notificationService.pageMine(1, 10)).thenReturn(notificationPage);
        when(operationLogService.pageLogs(1L, "认证", "登录", start, end, 1, 10)).thenReturn(logPage);

        assertThat(basicDataController.departments().data()).isSameAs(departments);
        assertThat(basicDataController.projects(2L).data()).isSameAs(projects);
        assertThat(rbacController.roles().data()).isSameAs(roles);
        assertThat(rbacController.permissions().data()).isSameAs(permissions);
        assertThat(notificationController.pageMine(1, 10).data()).isSameAs(notificationPage);
        assertThat(operationLogController.pageLogs(1L, "认证", "登录", start, end, 1, 10).data())
                .isSameAs(logPage);
        assertThat(healthController.health().data()).isEqualTo(Map.of(
                "status", "UP", "service", "internal-outsourcing-management"));
        assertThat(uiController.index()).isEqualTo("redirect:/ui/login");
        assertThat(uiController.login()).isEqualTo("login");
        assertThat(uiController.register()).isEqualTo("register");
        assertThat(uiController.dashboard()).isEqualTo("dashboard");
        assertThat(uiController.applications()).isEqualTo("applications");
        assertThat(uiController.approvals()).isEqualTo("approvals");
        assertThat(uiController.workLogs()).isEqualTo("work-logs");
        assertThat(uiController.performances()).isEqualTo("performances");
        assertThat(uiController.users()).isEqualTo("users");
        assertThat(uiController.notifications()).isEqualTo("notifications");
        assertThat(uiController.operationLogs()).isEqualTo("operation-logs");
    }

    private UserVO userVO(Long id) {
        return new UserVO(id, "tester", "13800000001", "tester@example.com", "测试",
                2L, UserStatus.ENABLED.name(), Set.of("OUTSOURCER"), Set.of("application:create"),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private ApplicationVO applicationVO(Long id) {
        return new ApplicationVO(id, 3L, "tester", 2L, "测试部", 1L, "PTA",
                "功能测试", "申请原因", "PENDING", null, null, null, null,
                LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now());
    }

    private WorkLogVO workLogVO() {
        return new WorkLogVO(1L, 3L, "tester", "测试", 1L, "PTA",
                LocalDate.now(), "内容", "无", "完成",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private PerformanceRecordVO performanceVO() {
        return new PerformanceRecordVO(1L, 2L, "leader", 3L, "tester", "测试",
                1L, "PTA", "MONTH", "2026-08", "A", "表现稳定", true,
                null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private NotificationMessageVO notificationVO() {
        return new NotificationMessageVO(1L, "event-1", 1L, 3L, "APPLICATION_APPROVED",
                "标题", "内容", "SENT", 0, null, LocalDateTime.now(), LocalDateTime.now());
    }

    private OperationLogVO operationLogVO() {
        return new OperationLogVO(1L, 1L, "admin", "认证", "登录",
                "POST /api/auth/login", "{}", "SUCCESS", null, LocalDateTime.now());
    }
}

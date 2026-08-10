package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.dto.WorkLogCreateRequest;
import com.pta.outsourcing.dto.WorkLogUpdateRequest;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.entity.WorkLog;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.mapper.WorkLogMapper;
import com.pta.outsourcing.security.CurrentUser;
import com.pta.outsourcing.service.impl.WorkLogServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class WorkLogServiceImplTest {

    private WorkLogMapper workLogMapper;
    private SysUserMapper sysUserMapper;
    private ProjectMapper projectMapper;
    private WorkLogService workLogService;

    @BeforeEach
    void setUp() {
        workLogMapper = mock(WorkLogMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        projectMapper = mock(ProjectMapper.class);
        workLogService = new WorkLogServiceImpl(workLogMapper, sysUserMapper, projectMapper);
        CurrentUser currentUser = new CurrentUser(3L, "tester", Set.of("OUTSOURCER"),
                Set.of("worklog:create", "worklog:update:self"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                "token",
                Set.of(new SimpleGrantedAuthority("worklog:create"))
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateWorkLogForCurrentUser() {
        Project project = new Project();
        project.setId(1L);
        project.setProjectName("内部测试外包人员管理系统");
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(workLogMapper.insert(any(WorkLog.class))).thenAnswer(invocation -> {
            WorkLog workLog = invocation.getArgument(0);
            workLog.setId(9L);
            return 1;
        });
        WorkLog persisted = new WorkLog();
        persisted.setId(9L);
        persisted.setUserId(3L);
        persisted.setProjectId(1L);
        persisted.setWorkDate(LocalDate.of(2026, 8, 8));
        persisted.setWorkContent("完成回归测试");
        persisted.setCompletionStatus("已完成");
        when(workLogMapper.selectById(9L)).thenReturn(persisted);
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("tester");
        when(sysUserMapper.selectById(3L)).thenReturn(user);

        var result = workLogService.create(new WorkLogCreateRequest(
                1L,
                LocalDate.of(2026, 8, 8),
                "完成回归测试",
                null,
                "已完成"
        ));

        assertThat(result.id()).isEqualTo(9L);
        assertThat(result.userId()).isEqualTo(3L);
        verify(workLogMapper).insert(any(WorkLog.class));
    }

    @Test
    void shouldRejectUpdatingOtherUsersWorkLog() {
        Project project = new Project();
        project.setId(1L);
        when(projectMapper.selectById(1L)).thenReturn(project);
        WorkLog workLog = new WorkLog();
        workLog.setId(7L);
        workLog.setUserId(4L);
        workLog.setProjectId(1L);
        when(workLogMapper.selectById(7L)).thenReturn(workLog);

        assertThatThrownBy(() -> workLogService.update(7L, new WorkLogUpdateRequest(
                1L,
                LocalDate.of(2026, 8, 8),
                "补充日志",
                null,
                "已完成"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能修改自己的工作日志");
    }

    @Test
    void shouldUpdateOwnWorkLog() {
        Project project = new Project();
        project.setId(1L);
        when(projectMapper.selectById(1L)).thenReturn(project);
        WorkLog workLog = new WorkLog();
        workLog.setId(7L);
        workLog.setUserId(3L);
        workLog.setProjectId(1L);
        workLog.setWorkDate(LocalDate.of(2026, 8, 8));
        workLog.setWorkContent("旧日志");
        workLog.setCompletionStatus("进行中");
        when(workLogMapper.selectById(7L)).thenReturn(workLog);
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("tester");
        when(sysUserMapper.selectById(3L)).thenReturn(user);

        var result = workLogService.update(7L, new WorkLogUpdateRequest(
                1L,
                LocalDate.of(2026, 8, 9),
                "新日志",
                "无",
                "已完成"
        ));

        assertThat(result.workContent()).isEqualTo("新日志");
        verify(workLogMapper).updateById(workLog);
    }

    @Test
    void shouldPageAllWorkLogsWithFilters() {
        WorkLog workLog = new WorkLog();
        workLog.setId(1L);
        workLog.setUserId(3L);
        workLog.setProjectId(1L);
        workLog.setWorkDate(LocalDate.of(2026, 8, 8));
        workLog.setWorkContent("完成测试");
        workLog.setCompletionStatus("已完成");
        Page<WorkLog> page = new Page<>(1, 10);
        page.setRecords(List.of(workLog));
        page.setTotal(1);
        when(workLogMapper.selectPage(any(), any())).thenReturn(page);
        Project project = new Project();
        project.setId(1L);
        project.setProjectName("内部测试外包人员管理系统");
        when(projectMapper.selectById(1L)).thenReturn(project);
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("tester");
        when(sysUserMapper.selectById(3L)).thenReturn(user);

        var result = workLogService.pageAll(
                3L,
                1L,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                1,
                10
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().get(0).projectName()).isEqualTo("内部测试外包人员管理系统");
    }

    @Test
    void shouldNormalizePageBoundsBeforeQueryingWorkLogs() {
        Page<WorkLog> page = new Page<>(1, 100);
        page.setRecords(List.of());
        page.setTotal(0);
        when(workLogMapper.selectPage(any(), any())).thenReturn(page);

        var result = workLogService.pageAll(null, null, null, null, 0, Long.MAX_VALUE);

        ArgumentCaptor<Page<WorkLog>> pageCaptor = ArgumentCaptor.captor();
        verify(workLogMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
        assertThat(result.pageNo()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(100);
    }
}

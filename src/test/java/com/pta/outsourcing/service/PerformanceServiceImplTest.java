package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.dto.PerformanceCreateRequest;
import com.pta.outsourcing.dto.PerformanceUpdateRequest;
import com.pta.outsourcing.entity.PerformanceRecord;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.mapper.PerformanceRecordMapper;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.security.CurrentUser;
import com.pta.outsourcing.service.impl.PerformanceServiceImpl;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PerformanceServiceImplTest {

    private PerformanceRecordMapper performanceRecordMapper;
    private SysUserMapper sysUserMapper;
    private ProjectMapper projectMapper;
    private RbacService rbacService;
    private RedisLockService redisLockService;
    private PerformanceService performanceService;

    @BeforeEach
    void setUp() {
        performanceRecordMapper = mock(PerformanceRecordMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        projectMapper = mock(ProjectMapper.class);
        rbacService = mock(RbacService.class);
        redisLockService = mock(RedisLockService.class);
        performanceService = new PerformanceServiceImpl(
                performanceRecordMapper,
                sysUserMapper,
                projectMapper,
                rbacService,
                redisLockService
        );
        CurrentUser currentUser = new CurrentUser(2L, "leader", Set.of("LEADER"),
                Set.of("performance:read", "performance:write"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                "token",
                Set.of(new SimpleGrantedAuthority("performance:write"))
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectNonOutsourcerWhenCreatingPerformance() {
        SysUser admin = user(1L, "admin", "系统管理员");
        when(sysUserMapper.selectById(1L)).thenReturn(admin);
        when(rbacService.listRoleCodesByUserId(1L)).thenReturn(Set.of("ADMIN"));
        Project project = new Project();
        project.setId(1L);
        project.setProjectName("内部测试外包人员管理系统");
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(redisLockService.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(performanceRecordMapper.insert(any(PerformanceRecord.class))).thenAnswer(invocation -> {
            PerformanceRecord record = invocation.getArgument(0);
            record.setId(9L);
            return 1;
        });
        when(performanceRecordMapper.selectById(9L)).thenReturn(currentRecord(9L));

        assertThatThrownBy(() -> performanceService.create(new PerformanceCreateRequest(
                1L,
                1L,
                "MONTH",
                "2026-08",
                "A",
                "不能评价管理员"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("绩效被评定人只能是测试外包人员");
    }

    @Test
    void shouldRejectCreateWhenCurrentPerformanceAlreadyExists() {
        seedUserAndProject();
        when(redisLockService.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        PerformanceRecord current = new PerformanceRecord();
        current.setId(1L);
        current.setEvaluatedUserId(3L);
        current.setProjectId(1L);
        current.setPeriodType("MONTH");
        current.setPeriodValue("2026-08");
        current.setCurrent(true);
        when(performanceRecordMapper.selectOne(any())).thenReturn(current);

        assertThatThrownBy(() -> performanceService.create(new PerformanceCreateRequest(
                3L,
                1L,
                "MONTH",
                "2026-08",
                "A",
                "表现稳定"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("该周期已有当前绩效，请使用修改功能并填写修改原因");

        verify(performanceRecordMapper, never()).updateById(any(PerformanceRecord.class));
        verify(performanceRecordMapper, never()).insert(any(PerformanceRecord.class));
        verify(redisLockService).release(anyString(), anyString());
    }

    @Test
    void shouldRejectWhenPerformanceLockIsBusy() {
        seedUserAndProject();
        when(redisLockService.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> performanceService.create(new PerformanceCreateRequest(
                3L,
                1L,
                "MONTH",
                "2026-08",
                "A",
                "表现稳定"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("绩效正在被修改，请稍后重试");
    }

    @Test
    void shouldReturnBusinessErrorWhenDatabaseRejectsDuplicateCurrentPerformance() {
        seedUserAndProject();
        when(redisLockService.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(performanceRecordMapper.insert(any(PerformanceRecord.class))).thenThrow(
                new DuplicateKeyException("uk_performance_current_active")
        );

        assertThatThrownBy(() -> performanceService.create(new PerformanceCreateRequest(
                3L,
                1L,
                "MONTH",
                "2026-08",
                "A",
                "表现稳定"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("同一人员同一项目同一周期只能有一条当前有效绩效记录");
        verify(redisLockService).release(anyString(), anyString());
    }

    @Test
    void shouldRequireModificationReasonWhenUpdatingPerformance() {
        PerformanceUpdateRequest request = new PerformanceUpdateRequest("A", "补充评价", "");

        assertThatThrownBy(() -> performanceService.update(1L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("修改绩效时必须填写修改原因");
    }

    @Test
    void shouldUpdateCurrentPerformanceByCreatingReplacementVersion() {
        seedUserAndProject();
        when(redisLockService.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        PerformanceRecord current = currentRecord(1L);
        PerformanceRecord replacement = currentRecord(4L);
        replacement.setGrade("B");
        replacement.setComment("调整后评价");
        replacement.setModificationReason("补充验收结果");
        when(performanceRecordMapper.selectById(1L)).thenReturn(current, current);
        when(performanceRecordMapper.insert(any(PerformanceRecord.class))).thenAnswer(invocation -> {
            PerformanceRecord record = invocation.getArgument(0);
            record.setId(4L);
            return 1;
        });
        when(performanceRecordMapper.selectById(4L)).thenReturn(replacement);

        var result = performanceService.update(1L, new PerformanceUpdateRequest(
                "B",
                "调整后评价",
                "补充验收结果"
        ));

        verify(performanceRecordMapper).updateById(current);
        verify(performanceRecordMapper).insert(any(PerformanceRecord.class));
        verify(redisLockService).release(anyString(), anyString());
        org.assertj.core.api.Assertions.assertThat(result.grade()).isEqualTo("B");
    }

    @Test
    void shouldPagePerformanceRecords() {
        seedUserAndProject();
        PerformanceRecord record = currentRecord(1L);
        Page<PerformanceRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1);
        when(performanceRecordMapper.selectPage(any(), any())).thenReturn(page);

        var result = performanceService.pageRecords(3L, List.of(), 1L, "MONTH", "2026-08", true, 1, 10);

        org.assertj.core.api.Assertions.assertThat(result.total()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(result.records().get(0).periodValue()).isEqualTo("2026-08");
    }

    @Test
    void shouldNormalizePageBoundsBeforeQueryingPerformanceRecords() {
        seedUserAndProject();
        Page<PerformanceRecord> page = new Page<>(1, 100);
        page.setRecords(List.of());
        page.setTotal(0);
        when(performanceRecordMapper.selectPage(any(), any())).thenReturn(page);

        var result = performanceService.pageRecords(3L, List.of(), 1L, "MONTH", "2026-08", true, -1, 500);

        ArgumentCaptor<Page<PerformanceRecord>> pageCaptor = ArgumentCaptor.captor();
        verify(performanceRecordMapper).selectPage(pageCaptor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
        org.assertj.core.api.Assertions.assertThat(result.pageNo()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(result.pageSize()).isEqualTo(100);
    }

    @Test
    void shouldPagePerformanceRecordsByMultipleEvaluatedUserIds() {
        seedUserAndProject();
        PerformanceRecord first = currentRecord(1L);
        PerformanceRecord second = currentRecord(2L);
        second.setEvaluatedUserId(8L);
        Page<PerformanceRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(first, second));
        page.setTotal(2);
        when(performanceRecordMapper.selectPage(any(), any())).thenReturn(page);

        var result = performanceService.pageRecords(null, List.of(3L, 8L), 1L, "MONTH", "2026-08", true, 1, 10);

        org.assertj.core.api.Assertions.assertThat(result.total()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(result.records())
                .extracting("evaluatedUserId")
                .containsExactly(3L, 8L);
    }

    @Test
    void shouldSearchUserOptionsByFuzzyRealName() {
        SysUser admin = user(1L, "admin", "系统管理员");
        SysUser leader = user(2L, "leader", "审批领导");
        SysUser first = user(3L, "tester03", "张三");
        SysUser second = user(8L, "tester08", "张三");
        when(sysUserMapper.selectList(any())).thenReturn(List.of(admin, leader, first, second));
        when(rbacService.listRoleCodesByUserId(1L)).thenReturn(Set.of("ADMIN"));
        when(rbacService.listRoleCodesByUserId(2L)).thenReturn(Set.of("LEADER"));
        when(rbacService.listRoleCodesByUserId(3L)).thenReturn(Set.of(RbacService.DEFAULT_OUTSOURCER_ROLE));
        when(rbacService.listRoleCodesByUserId(8L)).thenReturn(Set.of(RbacService.DEFAULT_OUTSOURCER_ROLE));

        var result = performanceService.searchUserOptions("张三", null);

        org.assertj.core.api.Assertions.assertThat(result)
                .extracting("id")
                .containsExactly(3L, 8L);
        org.assertj.core.api.Assertions.assertThat(result)
                .extracting("realName")
                .containsExactly("张三", "张三");
    }

    @Test
    void shouldSearchUserOptionsByUserId() {
        when(sysUserMapper.selectList(any())).thenReturn(List.of(user(3L, "tester03", "张三")));
        when(rbacService.listRoleCodesByUserId(3L)).thenReturn(Set.of(RbacService.DEFAULT_OUTSOURCER_ROLE));

        var result = performanceService.searchUserOptions(null, 3L);

        org.assertj.core.api.Assertions.assertThat(result)
                .extracting("id", "realName")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3L, "张三"));
    }

    @Test
    void shouldReturnEmptyUserOptionsWithoutCriteria() {
        var result = performanceService.searchUserOptions("", null);

        org.assertj.core.api.Assertions.assertThat(result).isEmpty();
        verifyNoInteractions(sysUserMapper);
    }

    @Test
    void shouldRejectInvalidQuarterPeriodValue() {
        assertThatThrownBy(() -> performanceService.pageRecords(3L, List.of(), 1L, "QUARTER", "2026-Q5", true, 1, 10))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("季度绩效周期值格式必须是 yyyy-Qn");
    }

    @Test
    void shouldRequireEvaluatedUserWhenQueryingHistory() {
        assertThatThrownBy(() -> performanceService.history(null, 1L, null, null, 1, 10))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("被评价用户不能为空");
    }

    private void seedUserAndProject() {
        SysUser user = user(3L, "tester", "测试外包人员");
        when(sysUserMapper.selectById(3L)).thenReturn(user);
        when(rbacService.listRoleCodesByUserId(3L)).thenReturn(Set.of(RbacService.DEFAULT_OUTSOURCER_ROLE));
        Project project = new Project();
        project.setId(1L);
        project.setProjectName("内部测试外包人员管理系统");
        when(projectMapper.selectById(1L)).thenReturn(project);
        SysUser leader = user(2L, "leader", "审批领导");
        when(sysUserMapper.selectById(2L)).thenReturn(leader);
    }

    private SysUser user(Long id, String username, String realName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setStatus("ENABLED");
        return user;
    }

    private PerformanceRecord currentRecord(Long id) {
        PerformanceRecord record = new PerformanceRecord();
        record.setId(id);
        record.setEvaluatorUserId(2L);
        record.setEvaluatedUserId(3L);
        record.setProjectId(1L);
        record.setPeriodType("MONTH");
        record.setPeriodValue("2026-08");
        record.setGrade("A");
        record.setCurrent(true);
        return record;
    }
}

package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.dto.UserCreateRequest;
import com.pta.outsourcing.entity.SysDepartment;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.service.impl.UserServiceImpl;
import com.pta.outsourcing.vo.UserVO;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {

    private SysUserMapper sysUserMapper;
    private SysDepartmentMapper sysDepartmentMapper;
    private RbacService rbacService;
    private PasswordEncoder passwordEncoder;
    private UserService userService;
    private AtomicReference<SysUser> insertedUser;

    @BeforeEach
    void setUp() {
        sysUserMapper = org.mockito.Mockito.mock(SysUserMapper.class);
        sysDepartmentMapper = org.mockito.Mockito.mock(SysDepartmentMapper.class);
        rbacService = org.mockito.Mockito.mock(RbacService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        insertedUser = new AtomicReference<>();
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L);
            insertedUser.set(user);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));
        when(sysUserMapper.selectById(100L)).thenAnswer(invocation -> insertedUser.get());
        when(rbacService.listRoleCodesByUserId(100L)).thenReturn(Set.of("LEADER"));
        when(rbacService.listPermissionCodesByUserId(100L)).thenReturn(Set.of("approval:read"));
        userService = new UserServiceImpl(sysUserMapper, sysDepartmentMapper, passwordEncoder, rbacService);
    }

    @Test
    void shouldCreateInternalLeaderWithEncryptedPasswordAndRoles() {
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysDepartmentMapper.selectById(2L)).thenReturn(new SysDepartment());

        UserVO user = userService.create(new UserCreateRequest(
                "leader02",
                "Leader@123456",
                "13800000003",
                "leader02@example.com",
                "审批领导二号",
                2L,
                null,
                Set.of(2L)
        ));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        SysUser savedUser = captor.getValue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Leader@123456");
        assertThat(passwordEncoder.matches("Leader@123456", savedUser.getPasswordHash())).isTrue();
        verify(rbacService).assignRoles(eq(100L), eq(Set.of(2L)));
        assertThat(user.username()).isEqualTo("leader02");
        assertThat(user.roles()).containsExactly("LEADER");
    }

    @Test
    void shouldRejectDuplicateUsernameWhenCreatingUser() {
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.create(new UserCreateRequest(
                "leader02",
                "Leader@123456",
                "13800000003",
                "leader02@example.com",
                "审批领导二号",
                2L,
                "ENABLED",
                Set.of(2L)
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("用户名已存在");

        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void shouldRejectInvalidDepartmentWhenCreatingUser() {
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysDepartmentMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> userService.create(new UserCreateRequest(
                "leader02",
                "Leader@123456",
                "13800000003",
                "leader02@example.com",
                "审批领导二号",
                999L,
                "ENABLED",
                Set.of(2L)
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("部门不存在");

        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }
}

package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.dto.LoginRequest;
import com.pta.outsourcing.dto.RegisterRequest;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.enums.UserStatus;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.security.JwtTokenProvider;
import com.pta.outsourcing.security.LoginSessionService;
import com.pta.outsourcing.service.impl.AuthServiceImpl;
import com.pta.outsourcing.vo.UserVO;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceImplTest {

    private SysUserMapper sysUserMapper;
    private RbacService rbacService;
    private LoginSessionService loginSessionService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        sysUserMapper = mock(SysUserMapper.class);
        rbacService = mock(RbacService.class);
        loginSessionService = mock(LoginSessionService.class);
        UserService userService = mock(UserService.class);
        when(userService.detail(100L)).thenReturn(new UserVO(
                100L,
                "tester01",
                "13800000002",
                "tester01@example.com",
                "测试外包一号",
                2L,
                UserStatus.ENABLED.name(),
                Set.of("OUTSOURCER"),
                Set.of("application:create"),
                null,
                null
        ));
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));
        authService = new AuthServiceImpl(
                sysUserMapper,
                new BCryptPasswordEncoder(),
                new JwtTokenProvider("test-secret-key-must-be-at-least-32-bytes-long", 60),
                loginSessionService,
                rbacService,
                userService
        );
    }

    @Test
    void shouldRegisterWithDefaultOutsourcerRole() {
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        authService.register(new RegisterRequest(
                "tester01",
                "Tester@123456",
                "13800000002",
                "tester01@example.com",
                "测试外包一号"
        ));

        verify(sysUserMapper).insert(any(SysUser.class));
        verify(rbacService).assignDefaultOutsourcerRole(100L);
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "tester01",
                "Tester@123456",
                "13800000002",
                "tester01@example.com",
                "测试外包一号"
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void shouldLoginAndSaveRedisSession() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        SysUser user = new SysUser();
        user.setId(100L);
        user.setUsername("tester01");
        user.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        user.setStatus(UserStatus.ENABLED.name());
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(rbacService.listRoleCodesByUserId(100L)).thenReturn(Set.of("OUTSOURCER"));
        when(rbacService.listPermissionCodesByUserId(100L)).thenReturn(Set.of("application:create"));

        authService.login(new LoginRequest("tester01", "Tester@123456"));

        verify(loginSessionService).save(eq(100L), any());
    }
}

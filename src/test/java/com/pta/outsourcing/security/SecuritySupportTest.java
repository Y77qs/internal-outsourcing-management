package com.pta.outsourcing.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.enums.UserStatus;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.service.RbacService;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SecuritySupportTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtAuthenticationFilterShouldAuthenticateValidCurrentSession() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-bytes-long", 60);
        JwtTokenProvider.JwtToken token = provider.generateToken(3L, "tester");
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        RbacService rbacService = mock(RbacService.class);
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("tester");
        user.setStatus(UserStatus.ENABLED.name());
        when(loginSessionService.isValid(3L, token.jti())).thenReturn(true);
        when(userMapper.selectById(3L)).thenReturn(user);
        when(rbacService.listRoleCodesByUserId(3L)).thenReturn(Set.of("OUTSOURCER"));
        when(rbacService.listPermissionCodesByUserId(3L)).thenReturn(Set.of("application:create"));
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(provider, loginSessionService, userMapper, rbacService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token.token());

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityUtils.hasPermission("application:create")).isTrue();
        assertThat(SecurityUtils.currentUser().username()).isEqualTo("tester");
    }

    @Test
    void jwtAuthenticationFilterShouldIgnoreMissingInvalidOrDisabledSessions() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-bytes-long", 60);
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        RbacService rbacService = mock(RbacService.class);
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(provider, loginSessionService, userMapper, rbacService);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
        invalidRequest.addHeader("Authorization", "Bearer invalid");
        filter.doFilter(invalidRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        JwtTokenProvider.JwtToken token = provider.generateToken(3L, "tester");
        when(loginSessionService.isValid(3L, token.jti())).thenReturn(false);
        MockHttpServletRequest expiredRequest = new MockHttpServletRequest();
        expiredRequest.addHeader("Authorization", "Bearer " + token.token());
        filter.doFilter(expiredRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        when(loginSessionService.isValid(3L, token.jti())).thenReturn(true);
        SysUser disabled = new SysUser();
        disabled.setId(3L);
        disabled.setStatus(UserStatus.DISABLED.name());
        when(userMapper.selectById(3L)).thenReturn(disabled);
        filter.doFilter(expiredRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void loginSessionServiceShouldPersistValidateAndRemoveLatestJti() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pta:login:jti:3")).thenReturn("jti-1");
        LoginSessionService service = new LoginSessionService(redisTemplate);
        ReflectionTestUtils.setField(service, "sessionPrefix", "pta:login:jti");
        ReflectionTestUtils.setField(service, "expirationMinutes", 120L);

        service.save(3L, "jti-1");
        assertThat(service.isValid(3L, "jti-1")).isTrue();
        assertThat(service.isValid(3L, "other")).isFalse();
        assertThat(service.isValid(3L, null)).isFalse();
        service.remove(3L);

        verify(valueOperations).set("pta:login:jti:3", "jti-1", Duration.ofMinutes(120));
        verify(redisTemplate).delete("pta:login:jti:3");
    }

    @Test
    void securityUtilsShouldRejectAnonymousAccessAndCheckAuthorities() {
        assertThatThrownBy(SecurityUtils::currentUser).isInstanceOf(BizException.class);
        assertThat(SecurityUtils.hasPermission("user:read")).isFalse();

        CurrentUser currentUser = new CurrentUser(1L, "admin", Set.of("ADMIN"), Set.of("user:read"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                "token",
                Set.of(new SimpleGrantedAuthority("user:read"))
        ));

        assertThat(SecurityUtils.currentUser()).isSameAs(currentUser);
        assertThat(SecurityUtils.hasPermission("user:read")).isTrue();
        assertThat(SecurityUtils.hasPermission("user:write")).isFalse();
    }
}

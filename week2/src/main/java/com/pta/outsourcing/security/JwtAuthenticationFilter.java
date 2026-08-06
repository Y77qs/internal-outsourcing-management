package com.pta.outsourcing.security;

import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.enums.UserStatus;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.service.RbacService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionService loginSessionService;
    private final SysUserMapper sysUserMapper;
    private final RbacService rbacService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            authenticateIfValid(token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateIfValid(String token) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            String jti = claims.getId();
            // JWT 只证明 Token 结构可信，Redis 中的 jti 才代表当前登录态仍然有效。
            if (!loginSessionService.isValid(userId, jti)) {
                return;
            }
            SysUser user = sysUserMapper.selectById(userId);
            // 用户被禁用后即使 Token 未过期，也不能继续访问受保护接口。
            if (user == null || !UserStatus.ENABLED.name().equals(user.getStatus())) {
                return;
            }
            Set<String> roles = rbacService.listRoleCodesByUserId(userId);
            Set<String> permissions = rbacService.listPermissionCodesByUserId(userId);
            // Spring Security 权限判断使用 GrantedAuthority，接口权限直接作为 authority，角色统一加 ROLE_ 前缀。
            Collection<SimpleGrantedAuthority> authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList());
            CurrentUser currentUser = new CurrentUser(userId, user.getUsername(), roles, permissions);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(currentUser, token, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exception) {
            log.debug("Ignore invalid JWT token: {}", exception.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}

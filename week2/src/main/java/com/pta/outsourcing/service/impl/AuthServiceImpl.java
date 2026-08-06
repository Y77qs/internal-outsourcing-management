package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.ErrorCode;
import com.pta.outsourcing.dto.LoginRequest;
import com.pta.outsourcing.dto.RegisterRequest;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.enums.UserStatus;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.security.JwtTokenProvider;
import com.pta.outsourcing.security.LoginSessionService;
import com.pta.outsourcing.security.SecurityUtils;
import com.pta.outsourcing.service.AuthService;
import com.pta.outsourcing.service.RbacService;
import com.pta.outsourcing.service.UserService;
import com.pta.outsourcing.vo.LoginResponse;
import com.pta.outsourcing.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionService loginSessionService;
    private final RbacService rbacService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        Long count = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRealName(request.realName());
        user.setStatus(UserStatus.ENABLED.name());
        sysUserMapper.insert(user);
        rbacService.assignDefaultOutsourcerRole(user.getId());
        return userService.detail(user.getId());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }
        JwtTokenProvider.JwtToken jwtToken = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        loginSessionService.save(user.getId(), jwtToken.jti());
        return new LoginResponse(
                "Bearer",
                jwtToken.token(),
                jwtToken.expiresIn(),
                user.getId(),
                user.getUsername(),
                rbacService.listRoleCodesByUserId(user.getId()),
                rbacService.listPermissionCodesByUserId(user.getId())
        );
    }

    @Override
    public void logout(String authorizationHeader) {
        var currentUser = SecurityUtils.currentUser();
        loginSessionService.remove(currentUser.id());
    }

    @Override
    public UserVO currentUser() {
        return userService.detail(SecurityUtils.currentUser().id());
    }
}

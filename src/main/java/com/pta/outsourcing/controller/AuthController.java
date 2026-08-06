package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.dto.LoginRequest;
import com.pta.outsourcing.dto.RegisterRequest;
import com.pta.outsourcing.service.AuthService;
import com.pta.outsourcing.vo.LoginResponse;
import com.pta.outsourcing.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册测试外包人员账号，成功后默认分配 OUTSOURCER 角色。
     *
     * @param request 注册请求体，包含用户名、密码、手机号、邮箱和真实姓名。
     * @return 注册成功后的用户信息、默认角色和权限集合。
     */
    @Operation(summary = "用户注册")
    @OperationLog(moduleName = "认证", operationType = "用户注册")
    @PostMapping("/register")
    public ResultVO<UserVO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "测试外包人员注册请求", required = true)
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResultVO.success(authService.register(request));
    }

    /**
     * 使用用户名和密码登录，成功后返回 JWT Token。
     *
     * @param request 登录请求体，包含用户名和密码。
     * @return 登录成功后的 Bearer Token、用户 ID、角色和权限。
     */
    @Operation(summary = "用户登录")
    @OperationLog(moduleName = "认证", operationType = "用户登录")
    @PostMapping("/login")
    public ResultVO<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户名密码登录请求", required = true)
            @Valid @RequestBody LoginRequest request
    ) {
        return ResultVO.success(authService.login(request));
    }

    /**
     * 退出登录并删除 Redis 中的当前登录态。
     *
     * @param authorization 当前登录用户的 Bearer Token，请求头可为空但为空时会按未登录处理。
     * @return 空响应，`code=00000` 表示退出成功。
     */
    @Operation(summary = "退出登录")
    @OperationLog(moduleName = "认证", operationType = "退出登录")
    @PostMapping("/logout")
    public ResultVO<Void> logout(
            @Parameter(description = "Bearer Token，例如：Bearer eyJhbGciOiJIUzI1NiJ9...", required = false)
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        authService.logout(authorization);
        return ResultVO.success();
    }

    /**
     * 查询当前登录用户信息、角色和权限。
     *
     * @return 当前登录用户详情、角色编码和权限编码。
     */
    @Operation(summary = "当前登录用户")
    @OperationLog(moduleName = "认证", operationType = "查询当前用户")
    @GetMapping("/me")
    public ResultVO<UserVO> currentUser() {
        return ResultVO.success(authService.currentUser());
    }
}

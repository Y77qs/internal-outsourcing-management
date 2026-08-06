package com.pta.outsourcing.service;

import com.pta.outsourcing.dto.LoginRequest;
import com.pta.outsourcing.dto.RegisterRequest;
import com.pta.outsourcing.vo.LoginResponse;
import com.pta.outsourcing.vo.UserVO;

public interface AuthService {

    /**
     * 注册测试外包人员账号，并分配默认外包人员角色。
     *
     * @param request 注册请求。
     * @return 注册后的用户信息。
     */
    UserVO register(RegisterRequest request);

    /**
     * 校验用户名密码，生成 JWT 并写入 Redis 登录态。
     *
     * @param request 登录请求。
     * @return 登录响应，包含 Token、角色和权限。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 删除当前用户 Redis 登录态，使现有 Token 失效。
     *
     * @param authorizationHeader Authorization 请求头。
     */
    void logout(String authorizationHeader);

    /**
     * 查询当前登录用户详情。
     *
     * @return 当前登录用户信息。
     */
    UserVO currentUser();
}

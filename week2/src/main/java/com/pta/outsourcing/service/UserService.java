package com.pta.outsourcing.service;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.UserCreateRequest;
import com.pta.outsourcing.dto.UserRoleUpdateRequest;
import com.pta.outsourcing.dto.UserStatusUpdateRequest;
import com.pta.outsourcing.vo.UserVO;

public interface UserService {

    /**
     * 管理员创建内部用户账号。
     *
     * @param request 创建用户请求，包含基础资料、初始密码和角色 ID 集合。
     * @return 创建后的用户详情。
     */
    UserVO create(UserCreateRequest request);

    /**
     * 管理员分页查询用户。
     *
     * @param username 用户名模糊查询条件。
     * @param status 用户状态筛选条件。
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 用户分页数据。
     */
    PageVO<UserVO> pageUsers(String username, String status, long pageNo, long pageSize);

    /**
     * 查询用户详情。
     *
     * @param userId 用户 ID。
     * @return 用户详情。
     */
    UserVO detail(Long userId);

    /**
     * 更新用户启用或禁用状态。
     *
     * @param userId 用户 ID。
     * @param request 状态更新请求。
     * @return 更新后的用户详情。
     */
    UserVO updateStatus(Long userId, UserStatusUpdateRequest request);

    /**
     * 更新用户角色集合。
     *
     * @param userId 用户 ID。
     * @param request 角色分配请求。
     * @return 更新后的用户详情。
     */
    UserVO updateRoles(Long userId, UserRoleUpdateRequest request);
}

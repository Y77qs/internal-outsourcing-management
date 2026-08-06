package com.pta.outsourcing.service;

import com.pta.outsourcing.vo.PermissionVO;
import com.pta.outsourcing.vo.RoleVO;
import java.util.List;
import java.util.Set;

public interface RbacService {

    String DEFAULT_OUTSOURCER_ROLE = "OUTSOURCER";
    String LEADER_ROLE = "LEADER";

    /**
     * 查询用户拥有的角色编码集合。
     *
     * @param userId 用户 ID。
     * @return 角色编码集合。
     */
    Set<String> listRoleCodesByUserId(Long userId);

    /**
     * 查询用户通过角色获得的权限编码集合。
     *
     * @param userId 用户 ID。
     * @return 权限编码集合。
     */
    Set<String> listPermissionCodesByUserId(Long userId);

    /**
     * 查询系统角色列表。
     *
     * @return 角色响应列表。
     */
    List<RoleVO> listRoles();

    /**
     * 查询系统权限列表。
     *
     * @return 权限响应列表。
     */
    List<PermissionVO> listPermissions();

    /**
     * 为新注册用户分配默认测试外包人员角色。
     *
     * @param userId 用户 ID。
     */
    void assignDefaultOutsourcerRole(Long userId);

    /**
     * 覆盖式分配用户角色。
     *
     * @param userId 用户 ID。
     * @param roleIds 角色 ID 集合。
     */
    void assignRoles(Long userId, Set<Long> roleIds);

    /**
     * 查询指定角色下第一个启用用户 ID。
     *
     * @param roleCode 角色编码。
     * @return 启用用户 ID，未找到时返回 null。
     */
    Long findFirstEnabledUserIdByRoleCode(String roleCode);
}

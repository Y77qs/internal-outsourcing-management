package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.ErrorCode;
import com.pta.outsourcing.entity.SysPermission;
import com.pta.outsourcing.entity.SysRole;
import com.pta.outsourcing.entity.SysRolePermission;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.entity.SysUserRole;
import com.pta.outsourcing.enums.UserStatus;
import com.pta.outsourcing.mapper.SysPermissionMapper;
import com.pta.outsourcing.mapper.SysRoleMapper;
import com.pta.outsourcing.mapper.SysRolePermissionMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.mapper.SysUserRoleMapper;
import com.pta.outsourcing.service.RbacService;
import com.pta.outsourcing.vo.PermissionVO;
import com.pta.outsourcing.vo.RoleVO;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private static final String ENABLED = "ENABLED";

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public Set<String> listRoleCodesByUserId(Long userId) {
        Set<Long> roleIds = listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        return sysRoleMapper.selectByIds(roleIds).stream()
                .filter(role -> ENABLED.equals(role.getStatus()))
                .map(SysRole::getRoleCode)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> listPermissionCodesByUserId(Long userId) {
        Set<Long> roleIds = listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        // RBAC 采用“用户-角色-权限”两级关联，先取角色 ID，再汇总角色拥有的权限 ID。
        Set<Long> permissionIds = sysRolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .in(SysRolePermission::getRoleId, roleIds))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return Collections.emptySet();
        }
        return sysPermissionMapper.selectByIds(permissionIds).stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toSet());
    }

    @Override
    public List<RoleVO> listRoles() {
        return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId))
                .stream()
                .map(role -> new RoleVO(
                        role.getId(),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getDescription(),
                        role.getStatus()
                ))
                .toList();
    }

    @Override
    public List<PermissionVO> listPermissions() {
        return sysPermissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                        .orderByAsc(SysPermission::getModuleName, SysPermission::getId))
                .stream()
                .map(permission -> new PermissionVO(
                        permission.getId(),
                        permission.getPermissionCode(),
                        permission.getPermissionName(),
                        permission.getModuleName(),
                        permission.getPermissionType(),
                        permission.getApiPath(),
                        permission.getHttpMethod()
                ))
                .toList();
    }

    @Override
    public void assignDefaultOutsourcerRole(Long userId) {
        SysRole role = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, DEFAULT_OUTSOURCER_ROLE));
        if (role == null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "默认测试外包人员角色不存在");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        sysUserRoleMapper.insert(userRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色 ID 集合不能为空");
        }
        if (sysUserMapper.selectById(userId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        List<SysRole> roles = sysRoleMapper.selectByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "存在无效角色 ID");
        }
        // 角色分配使用先删后插，保证请求体中的角色集合就是用户最终角色集合。
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    @Override
    public Long findFirstEnabledUserIdByRoleCode(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, roleCode)
                .eq(SysRole::getStatus, ENABLED));
        if (role == null) {
            return null;
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, role.getId()));
        // 当前审批模型采用单级审批，通知只需要找到第一个启用的领导账号即可。
        for (SysUserRole userRole : userRoles) {
            SysUser user = sysUserMapper.selectById(userRole.getUserId());
            if (user != null && UserStatus.ENABLED.name().equals(user.getStatus())) {
                return user.getId();
            }
        }
        return null;
    }

    private Set<Long> listRoleIdsByUserId(Long userId) {
        return sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}

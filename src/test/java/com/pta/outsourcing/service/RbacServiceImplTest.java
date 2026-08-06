package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.entity.SysRole;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.entity.SysUserRole;
import com.pta.outsourcing.mapper.SysPermissionMapper;
import com.pta.outsourcing.mapper.SysRoleMapper;
import com.pta.outsourcing.mapper.SysRolePermissionMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.mapper.SysUserRoleMapper;
import com.pta.outsourcing.service.impl.RbacServiceImpl;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RbacServiceImplTest {

    private SysRoleMapper sysRoleMapper;
    private SysUserMapper sysUserMapper;
    private SysUserRoleMapper sysUserRoleMapper;
    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        sysRoleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper sysPermissionMapper = mock(SysPermissionMapper.class);
        sysUserRoleMapper = mock(SysUserRoleMapper.class);
        SysRolePermissionMapper sysRolePermissionMapper = mock(SysRolePermissionMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        rbacService = new RbacServiceImpl(
                sysRoleMapper,
                sysPermissionMapper,
                sysUserRoleMapper,
                sysRolePermissionMapper,
                sysUserMapper
        );
    }

    @Test
    void shouldRejectEmptyRoleSet() {
        assertThatThrownBy(() -> rbacService.assignRoles(100L, Set.of()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色 ID 集合不能为空");
    }

    @Test
    void shouldRejectInvalidRoleId() {
        SysUser user = new SysUser();
        user.setId(100L);
        SysRole role = new SysRole();
        role.setId(2L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);
        when(sysRoleMapper.selectByIds(Set.of(2L, 999L))).thenReturn(List.of(role));

        assertThatThrownBy(() -> rbacService.assignRoles(100L, Set.of(2L, 999L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("存在无效角色 ID");
    }

    @Test
    void shouldReplaceUserRolesWhenRoleIdsAreValid() {
        SysUser user = new SysUser();
        user.setId(100L);
        SysRole role = new SysRole();
        role.setId(2L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);
        when(sysRoleMapper.selectByIds(Set.of(2L))).thenReturn(List.of(role));

        rbacService.assignRoles(100L, Set.of(2L));

        verify(sysUserRoleMapper).delete(any());
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
    }
}

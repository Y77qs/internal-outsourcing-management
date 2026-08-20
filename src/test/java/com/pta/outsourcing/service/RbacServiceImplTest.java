package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pta.outsourcing.common.BizException;
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
import com.pta.outsourcing.service.impl.RbacServiceImpl;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RbacServiceImplTest {

    private SysRoleMapper sysRoleMapper;
    private SysPermissionMapper sysPermissionMapper;
    private SysUserMapper sysUserMapper;
    private SysUserRoleMapper sysUserRoleMapper;
    private SysRolePermissionMapper sysRolePermissionMapper;
    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        initTableInfo(SysRolePermission.class);
        sysRoleMapper = mock(SysRoleMapper.class);
        sysPermissionMapper = mock(SysPermissionMapper.class);
        sysUserRoleMapper = mock(SysUserRoleMapper.class);
        sysRolePermissionMapper = mock(SysRolePermissionMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        rbacService = new RbacServiceImpl(
                sysRoleMapper,
                sysPermissionMapper,
                sysUserRoleMapper,
                sysRolePermissionMapper,
                sysUserMapper
        );
    }

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
        }
    }

    @Test
    void shouldRejectEmptyRoleSet() {
        assertThatThrownBy(() -> rbacService.assignRoles(100L, Set.of()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色 ID 集合不能为空");
    }

    @Test
    void shouldRejectMissingUserWhenAssigningRoles() {
        when(sysUserMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> rbacService.assignRoles(404L, Set.of(2L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户不存在");
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

    @Test
    void shouldListEnabledRoleCodesAndReturnEmptyWhenNoRoles() {
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(1L);
        SysRole enabled = role(1L, "ADMIN", UserStatus.ENABLED.name());
        SysRole disabled = role(2L, "LEADER", UserStatus.DISABLED.name());
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(sysRoleMapper.selectByIds(Set.of(1L))).thenReturn(List.of(enabled, disabled));

        assertThat(rbacService.listRoleCodesByUserId(100L)).containsExactly("ADMIN");

        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());
        assertThat(rbacService.listRoleCodesByUserId(100L)).isEmpty();
    }

    @Test
    void shouldListPermissionCodesThroughUserRoles() {
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(1L);
        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setPermissionId(10L);
        SysPermission permission = permission(10L, "user:read");
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(sysRolePermissionMapper.selectList(any())).thenReturn(List.of(rolePermission));
        when(sysPermissionMapper.selectByIds(Set.of(10L))).thenReturn(List.of(permission));

        assertThat(rbacService.listPermissionCodesByUserId(100L)).containsExactly("user:read");

        when(sysRolePermissionMapper.selectList(any())).thenReturn(List.of());
        assertThat(rbacService.listPermissionCodesByUserId(100L)).isEmpty();
    }

    @Test
    void shouldMapRolesAndPermissionsToViewObjects() {
        SysRole role = role(1L, "ADMIN", UserStatus.ENABLED.name());
        role.setRoleName("管理员");
        role.setDescription("系统管理员");
        SysPermission permission = permission(10L, "user:read");
        permission.setPermissionName("查询用户");
        permission.setModuleName("用户管理");
        permission.setPermissionType("API");
        permission.setApiPath("/api/users");
        permission.setHttpMethod("GET");
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role));
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(permission));

        assertThat(rbacService.listRoles()).singleElement()
                .satisfies(vo -> {
                    assertThat(vo.roleCode()).isEqualTo("ADMIN");
                    assertThat(vo.roleName()).isEqualTo("管理员");
                });
        assertThat(rbacService.listPermissions()).singleElement()
                .satisfies(vo -> {
                    assertThat(vo.permissionCode()).isEqualTo("user:read");
                    assertThat(vo.apiPath()).isEqualTo("/api/users");
                });
    }

    @Test
    void shouldAssignDefaultOutsourcerRoleOrRejectMissingDefaultRole() {
        when(sysRoleMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> rbacService.assignDefaultOutsourcerRole(100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("默认测试外包人员角色不存在");

        SysRole outsourcer = role(3L, RbacService.DEFAULT_OUTSOURCER_ROLE, UserStatus.ENABLED.name());
        when(sysRoleMapper.selectOne(any())).thenReturn(outsourcer);
        rbacService.assignDefaultOutsourcerRole(100L);

        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
    }

    @Test
    void shouldFindFirstEnabledUserByRoleCode() {
        SysRole leaderRole = role(2L, RbacService.LEADER_ROLE, UserStatus.ENABLED.name());
        SysUserRole disabledLeaderRelation = new SysUserRole();
        disabledLeaderRelation.setUserId(20L);
        SysUserRole enabledLeaderRelation = new SysUserRole();
        enabledLeaderRelation.setUserId(21L);
        SysUser disabledUser = new SysUser();
        disabledUser.setId(20L);
        disabledUser.setStatus(UserStatus.DISABLED.name());
        SysUser enabledUser = new SysUser();
        enabledUser.setId(21L);
        enabledUser.setStatus(UserStatus.ENABLED.name());
        when(sysRoleMapper.selectOne(any())).thenReturn(leaderRole);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(disabledLeaderRelation, enabledLeaderRelation));
        when(sysUserMapper.selectById(20L)).thenReturn(disabledUser);
        when(sysUserMapper.selectById(21L)).thenReturn(enabledUser);

        assertThat(rbacService.findFirstEnabledUserIdByRoleCode(RbacService.LEADER_ROLE)).isEqualTo(21L);

        when(sysRoleMapper.selectOne(any())).thenReturn(null);
        assertThat(rbacService.findFirstEnabledUserIdByRoleCode(RbacService.LEADER_ROLE)).isNull();
    }

    private SysRole role(Long id, String code, String status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(code);
        role.setStatus(status);
        return role;
    }

    private SysPermission permission(Long id, String code) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionCode(code);
        return permission;
    }
}

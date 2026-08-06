package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.ErrorCode;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.UserCreateRequest;
import com.pta.outsourcing.dto.UserRoleUpdateRequest;
import com.pta.outsourcing.dto.UserStatusUpdateRequest;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.enums.UserStatus;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.service.RbacService;
import com.pta.outsourcing.service.UserService;
import com.pta.outsourcing.vo.UserVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final PasswordEncoder passwordEncoder;
    private final RbacService rbacService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateRequest request) {
        Long count = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        validateDepartment(request.departmentId());
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRealName(request.realName());
        user.setDepartmentId(request.departmentId());
        user.setStatus(parseStatusOrDefault(request.status(), UserStatus.ENABLED).name());
        sysUserMapper.insert(user);
        // 内部领导和管理员不走公开注册，必须由管理员明确指定角色集合。
        rbacService.assignRoles(user.getId(), request.roleIds());
        return detail(user.getId());
    }

    @Override
    public PageVO<UserVO> pageUsers(String username, String status, long pageNo, long pageSize) {
        IPage<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<SysUser>lambdaQuery()
                        .like(StringUtils.isNotBlank(username), SysUser::getUsername, username)
                        .eq(StringUtils.isNotBlank(status), SysUser::getStatus, status)
                        .orderByDesc(SysUser::getCreatedAt));
        List<UserVO> records = page.getRecords().stream().map(this::toUserVO).toList();
        return new PageVO<>(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public UserVO detail(Long userId) {
        return toUserVO(requiredUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateStatus(Long userId, UserStatusUpdateRequest request) {
        UserStatus status = parseStatus(request.status());
        SysUser user = requiredUser(userId);
        user.setStatus(status.name());
        sysUserMapper.updateById(user);
        return detail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateRoles(Long userId, UserRoleUpdateRequest request) {
        rbacService.assignRoles(userId, request.roleIds());
        return detail(userId);
    }

    private SysUser requiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void validateDepartment(Long departmentId) {
        if (departmentId != null && sysDepartmentMapper.selectById(departmentId) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "部门不存在");
        }
    }

    private UserVO toUserVO(SysUser user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getRealName(),
                user.getDepartmentId(),
                user.getStatus(),
                rbacService.listRoleCodesByUserId(user.getId()),
                rbacService.listPermissionCodesByUserId(user.getId()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserStatus parseStatus(String status) {
        return parseStatusOrDefault(status, null);
    }

    private UserStatus parseStatusOrDefault(String status, UserStatus defaultStatus) {
        if (StringUtils.isBlank(status) && defaultStatus != null) {
            return defaultStatus;
        }
        if (StringUtils.isBlank(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户状态只能是 ENABLED 或 DISABLED");
        }
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户状态只能是 ENABLED 或 DISABLED");
        }
    }
}

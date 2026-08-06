package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.ErrorCode;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.OnboardingApplicationCreateRequest;
import com.pta.outsourcing.entity.OnboardingApplication;
import com.pta.outsourcing.enums.ApplicationStatus;
import com.pta.outsourcing.enums.NotificationType;
import com.pta.outsourcing.mapper.OnboardingApplicationMapper;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.security.SecurityUtils;
import com.pta.outsourcing.service.NotificationService;
import com.pta.outsourcing.service.OnboardingApplicationService;
import com.pta.outsourcing.service.RbacService;
import com.pta.outsourcing.vo.ApplicationVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingApplicationServiceImpl implements OnboardingApplicationService {

    private final OnboardingApplicationMapper applicationMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final ProjectMapper projectMapper;
    private final ApplicationAssembler applicationAssembler;
    private final RbacService rbacService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationVO create(OnboardingApplicationCreateRequest request) {
        var currentUser = SecurityUtils.currentUser();
        if (sysDepartmentMapper.selectById(request.departmentId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "所属部门不存在");
        }
        if (projectMapper.selectById(request.projectId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        // 同一测试外包人员在同一项目下只允许存在一条待审批申请，避免领导重复处理。
        Long pendingCount = applicationMapper.selectCount(Wrappers.<OnboardingApplication>lambdaQuery()
                .eq(OnboardingApplication::getApplicantId, currentUser.id())
                .eq(OnboardingApplication::getProjectId, request.projectId())
                .eq(OnboardingApplication::getStatus, ApplicationStatus.PENDING.name()));
        if (pendingCount > 0) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "同一用户同一项目存在待审批申请，不能重复提交");
        }
        OnboardingApplication application = new OnboardingApplication();
        application.setApplicantId(currentUser.id());
        application.setDepartmentId(request.departmentId());
        application.setProjectId(request.projectId());
        application.setPositionType(request.positionType());
        application.setApplicationReason(request.applicationReason());
        application.setStatus(ApplicationStatus.PENDING.name());
        application.setSubmittedAt(LocalDateTime.now());
        applicationMapper.insert(application);

        Long leaderId = rbacService.findFirstEnabledUserIdByRoleCode(RbacService.LEADER_ROLE);
        // 申请提交后异步通知领导，审批提醒不阻塞主业务事务。
        notificationService.publishOnboardingEvent(
                application.getId(),
                currentUser.id(),
                leaderId,
                NotificationType.APPLICATION_SUBMITTED,
                "新的上岗申请待审批",
                "用户 " + currentUser.username() + " 提交了上岗申请，请及时处理。"
        );
        return applicationAssembler.toVO(applicationMapper.selectById(application.getId()));
    }

    @Override
    public PageVO<ApplicationVO> pageMine(long pageNo, long pageSize) {
        Long userId = SecurityUtils.currentUser().id();
        IPage<OnboardingApplication> page = applicationMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<OnboardingApplication>lambdaQuery()
                        .eq(OnboardingApplication::getApplicantId, userId)
                        .orderByDesc(OnboardingApplication::getCreatedAt));
        List<ApplicationVO> records = page.getRecords().stream().map(applicationAssembler::toVO).toList();
        return new PageVO<>(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public ApplicationVO detail(Long applicationId) {
        OnboardingApplication application = requiredApplication(applicationId);
        var currentUser = SecurityUtils.currentUser();
        boolean ownsApplication = currentUser.id().equals(application.getApplicantId());
        boolean canAudit = SecurityUtils.hasPermission("approval:read") || SecurityUtils.hasPermission("user:read");
        // 普通测试外包人员只能看自己的申请，领导和管理员可按审批/管理权限查看。
        if (!ownsApplication && !canAudit) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能查看自己的申请或审批范围内的申请");
        }
        return applicationAssembler.toVO(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationVO withdraw(Long applicationId) {
        OnboardingApplication application = requiredApplication(applicationId);
        var currentUser = SecurityUtils.currentUser();
        if (!currentUser.id().equals(application.getApplicantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能撤回自己的申请");
        }
        if (!ApplicationStatus.PENDING.name().equals(application.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "只有待审批申请可以撤回");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN.name());
        application.setWithdrawnAt(LocalDateTime.now());
        applicationMapper.updateById(application);
        Long leaderId = rbacService.findFirstEnabledUserIdByRoleCode(RbacService.LEADER_ROLE);
        // 撤回也要通知领导，避免领导端继续处理已撤回的申请。
        notificationService.publishOnboardingEvent(
                application.getId(),
                currentUser.id(),
                leaderId,
                NotificationType.APPLICATION_WITHDRAWN,
                "上岗申请已撤回",
                "用户 " + currentUser.username() + " 撤回了上岗申请。"
        );
        return applicationAssembler.toVO(applicationMapper.selectById(applicationId));
    }

    private OnboardingApplication requiredApplication(Long applicationId) {
        OnboardingApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "上岗申请不存在");
        }
        return application;
    }
}

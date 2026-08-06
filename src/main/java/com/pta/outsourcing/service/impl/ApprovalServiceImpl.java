package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.ErrorCode;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.ApprovalBatchRequest;
import com.pta.outsourcing.dto.ApprovalRequest;
import com.pta.outsourcing.entity.ApprovalRecord;
import com.pta.outsourcing.entity.OnboardingApplication;
import com.pta.outsourcing.enums.ApplicationStatus;
import com.pta.outsourcing.enums.ApprovalResult;
import com.pta.outsourcing.enums.NotificationType;
import com.pta.outsourcing.mapper.ApprovalRecordMapper;
import com.pta.outsourcing.mapper.OnboardingApplicationMapper;
import com.pta.outsourcing.security.SecurityUtils;
import com.pta.outsourcing.service.ApprovalService;
import com.pta.outsourcing.service.NotificationService;
import com.pta.outsourcing.vo.ApplicationVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final OnboardingApplicationMapper applicationMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApplicationAssembler applicationAssembler;
    private final NotificationService notificationService;

    @Override
    public PageVO<ApplicationVO> pagePending(long pageNo, long pageSize) {
        IPage<OnboardingApplication> page = applicationMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<OnboardingApplication>lambdaQuery()
                        .eq(OnboardingApplication::getStatus, ApplicationStatus.PENDING.name())
                        .orderByAsc(OnboardingApplication::getCreatedAt));
        List<ApplicationVO> records = page.getRecords().stream().map(applicationAssembler::toVO).toList();
        return new PageVO<>(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationVO approve(Long applicationId, ApprovalRequest request) {
        return process(applicationId, ApprovalResult.APPROVED, request.opinion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationVO reject(Long applicationId, ApprovalRequest request) {
        if (StringUtils.isBlank(request.opinion())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "驳回申请必须填写审批意见");
        }
        return process(applicationId, ApprovalResult.REJECTED, request.opinion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ApplicationVO> batchProcess(ApprovalBatchRequest request) {
        ApprovalResult result = parseResult(request.result());
        // 批量驳回与单个驳回保持同一业务规则，避免无意见驳回导致后续追溯困难。
        if (result == ApprovalResult.REJECTED && StringUtils.isBlank(request.opinion())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "批量驳回必须填写审批意见");
        }
        List<ApplicationVO> processed = new ArrayList<>();
        for (Long applicationId : request.applicationIds()) {
            processed.add(process(applicationId, result, request.opinion()));
        }
        return processed;
    }

    private ApplicationVO process(Long applicationId, ApprovalResult result, String opinion) {
        var currentUser = SecurityUtils.currentUser();
        OnboardingApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "上岗申请不存在");
        }
        if (!ApplicationStatus.PENDING.name().equals(application.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "只有待审批申请可以被审批");
        }
        // 审批是单级最终审批，申请状态和审批记录必须在同一事务中一起落库。
        application.setStatus(result == ApprovalResult.APPROVED
                ? ApplicationStatus.APPROVED.name()
                : ApplicationStatus.REJECTED.name());
        applicationMapper.updateById(application);

        ApprovalRecord approvalRecord = new ApprovalRecord();
        approvalRecord.setApplicationId(applicationId);
        approvalRecord.setApproverId(currentUser.id());
        approvalRecord.setResult(result.name());
        approvalRecord.setOpinion(opinion);
        approvalRecord.setApprovedAt(LocalDateTime.now());
        approvalRecordMapper.insert(approvalRecord);

        // 审批完成后通过 MQ 通知申请人，发送动作由通知服务保证事务提交后执行。
        notificationService.publishOnboardingEvent(
                applicationId,
                application.getApplicantId(),
                application.getApplicantId(),
                result == ApprovalResult.APPROVED
                        ? NotificationType.APPLICATION_APPROVED
                        : NotificationType.APPLICATION_REJECTED,
                result == ApprovalResult.APPROVED ? "上岗申请已通过" : "上岗申请已驳回",
                result == ApprovalResult.APPROVED
                        ? "你的上岗申请已通过。"
                        : "你的上岗申请已驳回，意见：" + opinion
        );
        return applicationAssembler.toVO(applicationMapper.selectById(applicationId));
    }

    private ApprovalResult parseResult(String result) {
        try {
            return ApprovalResult.valueOf(result);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.PARAM_ERROR, "审批结果只能是 APPROVED 或 REJECTED");
        }
    }
}

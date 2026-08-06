package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pta.outsourcing.entity.ApprovalRecord;
import com.pta.outsourcing.entity.OnboardingApplication;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysDepartment;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.mapper.ApprovalRecordMapper;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.vo.ApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationAssembler {

    private final SysUserMapper sysUserMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final ProjectMapper projectMapper;
    private final ApprovalRecordMapper approvalRecordMapper;

    public ApplicationVO toVO(OnboardingApplication application) {
        SysUser applicant = sysUserMapper.selectById(application.getApplicantId());
        SysDepartment department = sysDepartmentMapper.selectById(application.getDepartmentId());
        Project project = projectMapper.selectById(application.getProjectId());
        ApprovalRecord approvalRecord = approvalRecordMapper.selectOne(Wrappers.<ApprovalRecord>lambdaQuery()
                .eq(ApprovalRecord::getApplicationId, application.getId()));
        SysUser approver = approvalRecord == null ? null : sysUserMapper.selectById(approvalRecord.getApproverId());
        return new ApplicationVO(
                application.getId(),
                application.getApplicantId(),
                applicant == null ? null : applicant.getUsername(),
                application.getDepartmentId(),
                department == null ? null : department.getDepartmentName(),
                application.getProjectId(),
                project == null ? null : project.getProjectName(),
                application.getPositionType(),
                application.getApplicationReason(),
                application.getStatus(),
                approvalRecord == null ? null : approvalRecord.getResult(),
                approvalRecord == null ? null : approvalRecord.getOpinion(),
                approver == null ? null : approver.getUsername(),
                approvalRecord == null ? null : approvalRecord.getApprovedAt(),
                application.getSubmittedAt(),
                application.getWithdrawnAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}

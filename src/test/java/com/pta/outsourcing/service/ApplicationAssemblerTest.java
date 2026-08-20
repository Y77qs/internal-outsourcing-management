package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.entity.ApprovalRecord;
import com.pta.outsourcing.entity.OnboardingApplication;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysDepartment;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.enums.ApplicationStatus;
import com.pta.outsourcing.enums.ApprovalResult;
import com.pta.outsourcing.mapper.ApprovalRecordMapper;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.service.impl.ApplicationAssembler;
import com.pta.outsourcing.vo.ApplicationVO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ApplicationAssemblerTest {

    @Test
    void shouldAssembleApplicationWithRelatedNamesAndApprovalRecord() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDepartmentMapper departmentMapper = mock(SysDepartmentMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ApprovalRecordMapper approvalRecordMapper = mock(ApprovalRecordMapper.class);
        ApplicationAssembler assembler = new ApplicationAssembler(
                userMapper, departmentMapper, projectMapper, approvalRecordMapper);
        OnboardingApplication application = application();
        SysUser applicant = user(3L, "tester");
        SysUser approver = user(2L, "leader");
        SysDepartment department = new SysDepartment();
        department.setDepartmentName("测试部");
        Project project = new Project();
        project.setProjectName("PTA");
        ApprovalRecord approvalRecord = new ApprovalRecord();
        approvalRecord.setApproverId(2L);
        approvalRecord.setResult(ApprovalResult.APPROVED.name());
        approvalRecord.setOpinion("同意");
        approvalRecord.setApprovedAt(LocalDateTime.now());
        when(userMapper.selectById(3L)).thenReturn(applicant);
        when(userMapper.selectById(2L)).thenReturn(approver);
        when(departmentMapper.selectById(2L)).thenReturn(department);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(approvalRecordMapper.selectOne(any())).thenReturn(approvalRecord);

        ApplicationVO vo = assembler.toVO(application);

        assertThat(vo.applicantName()).isEqualTo("tester");
        assertThat(vo.departmentName()).isEqualTo("测试部");
        assertThat(vo.projectName()).isEqualTo("PTA");
        assertThat(vo.approverName()).isEqualTo("leader");
        assertThat(vo.approvalResult()).isEqualTo(ApprovalResult.APPROVED.name());
    }

    @Test
    void shouldTolerateMissingRelatedRows() {
        ApplicationAssembler assembler = new ApplicationAssembler(
                mock(SysUserMapper.class),
                mock(SysDepartmentMapper.class),
                mock(ProjectMapper.class),
                mock(ApprovalRecordMapper.class)
        );

        ApplicationVO vo = assembler.toVO(application());

        assertThat(vo.applicantName()).isNull();
        assertThat(vo.departmentName()).isNull();
        assertThat(vo.projectName()).isNull();
        assertThat(vo.approverName()).isNull();
    }

    private OnboardingApplication application() {
        OnboardingApplication application = new OnboardingApplication();
        application.setId(10L);
        application.setApplicantId(3L);
        application.setDepartmentId(2L);
        application.setProjectId(1L);
        application.setPositionType("功能测试");
        application.setApplicationReason("参与测试");
        application.setStatus(ApplicationStatus.PENDING.name());
        application.setSubmittedAt(LocalDateTime.now());
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        return application;
    }

    private SysUser user(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}

package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.ErrorCode;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.PageQuery;
import com.pta.outsourcing.dto.WorkLogCreateRequest;
import com.pta.outsourcing.dto.WorkLogUpdateRequest;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.entity.WorkLog;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.mapper.WorkLogMapper;
import com.pta.outsourcing.security.SecurityUtils;
import com.pta.outsourcing.service.WorkLogService;
import com.pta.outsourcing.vo.WorkLogVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkLogServiceImpl implements WorkLogService {

    private final WorkLogMapper workLogMapper;
    private final SysUserMapper sysUserMapper;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkLogVO create(WorkLogCreateRequest request) {
        validateProject(request.projectId());
        var currentUser = SecurityUtils.currentUser();
        WorkLog workLog = new WorkLog();
        workLog.setUserId(currentUser.id());
        workLog.setProjectId(request.projectId());
        workLog.setWorkDate(request.workDate());
        workLog.setWorkContent(request.workContent());
        workLog.setIssueRecord(request.issueRecord());
        workLog.setCompletionStatus(request.completionStatus());
        workLog.setSubmittedAt(LocalDateTime.now());
        workLogMapper.insert(workLog);
        return toVO(workLogMapper.selectById(workLog.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkLogVO update(Long workLogId, WorkLogUpdateRequest request) {
        validateProject(request.projectId());
        var currentUser = SecurityUtils.currentUser();
        WorkLog workLog = requiredWorkLog(workLogId);
        if (!currentUser.id().equals(workLog.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能修改自己的工作日志");
        }
        workLog.setProjectId(request.projectId());
        workLog.setWorkDate(request.workDate());
        workLog.setWorkContent(request.workContent());
        workLog.setIssueRecord(request.issueRecord());
        workLog.setCompletionStatus(request.completionStatus());
        workLog.setSubmittedAt(LocalDateTime.now());
        workLogMapper.updateById(workLog);
        return toVO(workLogMapper.selectById(workLogId));
    }

    @Override
    public PageVO<WorkLogVO> pageMine(
            LocalDate startDate,
            LocalDate endDate,
            Long projectId,
            long pageNo,
            long pageSize
    ) {
        Long userId = SecurityUtils.currentUser().id();
        return pageLogs(userId, projectId, startDate, endDate, pageNo, pageSize);
    }

    @Override
    public PageVO<WorkLogVO> pageAll(
            Long userId,
            Long projectId,
            LocalDate startDate,
            LocalDate endDate,
            long pageNo,
            long pageSize
    ) {
        return pageLogs(userId, projectId, startDate, endDate, pageNo, pageSize);
    }

    private PageVO<WorkLogVO> pageLogs(
            Long userId,
            Long projectId,
            LocalDate startDate,
            LocalDate endDate,
            long pageNo,
            long pageSize
    ) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize);
        IPage<WorkLog> page = workLogMapper.selectPage(new Page<>(pageQuery.pageNo(), pageQuery.pageSize()),
                Wrappers.<WorkLog>lambdaQuery()
                        .eq(userId != null, WorkLog::getUserId, userId)
                        .eq(projectId != null, WorkLog::getProjectId, projectId)
                        .ge(startDate != null, WorkLog::getWorkDate, startDate)
                        .le(endDate != null, WorkLog::getWorkDate, endDate)
                        .orderByDesc(WorkLog::getWorkDate, WorkLog::getId));
        List<WorkLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(records, page.getTotal(), pageQuery.pageNo(), pageQuery.pageSize());
    }

    private WorkLog requiredWorkLog(Long workLogId) {
        WorkLog workLog = workLogMapper.selectById(workLogId);
        if (workLog == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工作日志不存在");
        }
        return workLog;
    }

    private void validateProject(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
    }

    private WorkLogVO toVO(WorkLog workLog) {
        SysUser user = sysUserMapper.selectById(workLog.getUserId());
        Project project = projectMapper.selectById(workLog.getProjectId());
        return new WorkLogVO(
                workLog.getId(),
                workLog.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getRealName(),
                workLog.getProjectId(),
                project == null ? null : project.getProjectName(),
                workLog.getWorkDate(),
                workLog.getWorkContent(),
                workLog.getIssueRecord(),
                workLog.getCompletionStatus(),
                workLog.getSubmittedAt(),
                workLog.getCreatedAt(),
                workLog.getUpdatedAt()
        );
    }
}

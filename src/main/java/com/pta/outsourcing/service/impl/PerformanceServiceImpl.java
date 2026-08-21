package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pta.outsourcing.common.BizException;
import com.pta.outsourcing.common.ErrorCode;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.PageQuery;
import com.pta.outsourcing.dto.PerformanceCreateRequest;
import com.pta.outsourcing.dto.PerformanceUpdateRequest;
import com.pta.outsourcing.entity.PerformanceRecord;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysUser;
import com.pta.outsourcing.enums.PerformanceGrade;
import com.pta.outsourcing.enums.PerformancePeriodType;
import com.pta.outsourcing.mapper.PerformanceRecordMapper;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysUserMapper;
import com.pta.outsourcing.security.SecurityUtils;
import com.pta.outsourcing.service.PerformanceService;
import com.pta.outsourcing.service.RbacService;
import com.pta.outsourcing.service.RedisLockService;
import com.pta.outsourcing.vo.PerformanceRecordVO;
import com.pta.outsourcing.vo.PerformanceUserOptionVO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Pattern MONTH_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
    private static final Pattern QUARTER_PATTERN = Pattern.compile("\\d{4}-Q[1-4]");
    private static final String OUTSOURCER_USER_SQL = """
            SELECT sur.user_id
            FROM sys_user_role sur
            JOIN sys_role sr ON sr.id = sur.role_id
            WHERE sr.role_code = 'OUTSOURCER' AND sr.status = 'ENABLED'
            """;

    private final PerformanceRecordMapper performanceRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final ProjectMapper projectMapper;
    private final RbacService rbacService;
    private final RedisLockService redisLockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceRecordVO create(PerformanceCreateRequest request) {
        PerformancePeriodType periodType = parsePeriodType(request.periodType());
        PerformanceGrade grade = parseGrade(request.grade());
        SysUser evaluatedUser = requiredOutsourcerUser(request.evaluatedUserId());
        Project project = requiredProject(request.projectId());
        String periodValue = normalizePeriodValue(periodType, request.periodValue(), project.getId());
        String lockKey = lockKey(evaluatedUser.getId(), project.getId(), periodType.name(), periodValue);
        String token = UUID.randomUUID().toString();
        // 锁粒度按人员、项目和绩效周期拆分，避免同一当前绩效被并发创建或覆盖。
        if (!redisLockService.acquire(lockKey, token, LOCK_TTL)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "绩效正在被修改，请稍后重试");
        }
        try {
            PerformanceRecord current = findCurrent(evaluatedUser.getId(), project.getId(), periodType.name(),
                    periodValue);
            if (current != null) {
                throw new BizException(ErrorCode.BUSINESS_ERROR, "该周期已有当前绩效，请使用修改功能并填写修改原因");
            }
            PerformanceRecord record = buildRecord(
                    evaluatedUser.getId(),
                    project.getId(),
                    periodType.name(),
                    periodValue,
                    grade.name(),
                    request.comment(),
                    null
            );
            insertCurrentRecord(record);
            return toVO(performanceRecordMapper.selectById(record.getId()));
        } finally {
            redisLockService.release(lockKey, token);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceRecordVO update(Long performanceId, PerformanceUpdateRequest request) {
        if (StringUtils.isBlank(request.modificationReason())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "修改绩效时必须填写修改原因");
        }
        PerformanceGrade grade = parseGrade(request.grade());
        PerformanceRecord existing = requiredPerformance(performanceId);
        requiredOutsourcerUser(existing.getEvaluatedUserId());
        if (!Boolean.TRUE.equals(existing.getCurrent())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "只能修改当前有效绩效记录");
        }
        String lockKey = lockKey(existing.getEvaluatedUserId(), existing.getProjectId(), existing.getPeriodType(),
                existing.getPeriodValue());
        String token = UUID.randomUUID().toString();
        // 更新和新增共用同一把业务锁，保证归档旧记录与创建新记录之间不会被其他请求插入。
        if (!redisLockService.acquire(lockKey, token, LOCK_TTL)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "绩效正在被修改，请稍后重试");
        }
        try {
            // 修改不覆盖原记录，而是归档旧版本并生成新当前版本，保证历史可追溯。
            PerformanceRecord current = requiredPerformance(performanceId);
            if (!Boolean.TRUE.equals(current.getCurrent())) {
                throw new BizException(ErrorCode.BUSINESS_ERROR, "只能修改当前有效绩效记录");
            }
            current.setCurrent(false);
            current.setModificationReason(request.modificationReason());
            performanceRecordMapper.updateById(current);

            PerformanceRecord replacement = buildRecord(
                    current.getEvaluatedUserId(),
                    current.getProjectId(),
                    current.getPeriodType(),
                    current.getPeriodValue(),
                    grade.name(),
                    request.comment(),
                    request.modificationReason()
            );
            insertCurrentRecord(replacement);
            return toVO(performanceRecordMapper.selectById(replacement.getId()));
        } finally {
            redisLockService.release(lockKey, token);
        }
    }

    @Override
    public PageVO<PerformanceRecordVO> pageRecords(
            Long evaluatedUserId,
            List<Long> evaluatedUserIds,
            Long projectId,
            String periodType,
            String periodValue,
            Boolean current,
            long pageNo,
            long pageSize
    ) {
        PerformancePeriodType parsedPeriodType = StringUtils.isBlank(periodType) ? null : parsePeriodType(periodType);
        String normalizedPeriodValue = parsedPeriodType == null
                ? periodValue
                : normalizeOptionalPeriodValue(parsedPeriodType, periodValue, projectId);
        List<Long> normalizedUserIds = evaluatedUserId == null ? normalizeUserIds(evaluatedUserIds) : List.of();
        return page(evaluatedUserId, normalizedUserIds, projectId, parsedPeriodType, normalizedPeriodValue, current,
                pageNo, pageSize);
    }

    @Override
    public List<PerformanceUserOptionVO> searchUserOptions(String name, Long userId) {
        if (userId == null && StringUtils.isBlank(name)) {
            return List.of();
        }
        List<SysUser> users = sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(userId != null, SysUser::getId, userId)
                .like(userId == null && StringUtils.isNotBlank(name), SysUser::getRealName, name)
                .inSql(SysUser::getId, OUTSOURCER_USER_SQL)
                .orderByAsc(SysUser::getId)
                .last("LIMIT 20"));
        return users.stream()
                .filter(user -> isOutsourcer(user.getId()))
                .map(user -> new PerformanceUserOptionVO(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getStatus()
                ))
                .toList();
    }

    @Override
    public PerformanceRecordVO detail(Long performanceId) {
        return toVO(requiredPerformance(performanceId));
    }

    @Override
    public PageVO<PerformanceRecordVO> pageMine(Long projectId, Boolean current, long pageNo, long pageSize) {
        Long userId = SecurityUtils.currentUser().id();
        return page(userId, List.of(), projectId, null, null, current, pageNo, pageSize);
    }

    @Override
    public PageVO<PerformanceRecordVO> history(
            Long evaluatedUserId,
            Long projectId,
            String periodType,
            String periodValue,
            long pageNo,
            long pageSize
    ) {
        if (evaluatedUserId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "被评价用户不能为空");
        }
        PerformancePeriodType parsedPeriodType = StringUtils.isBlank(periodType) ? null : parsePeriodType(periodType);
        String normalizedPeriodValue = parsedPeriodType == null
                ? periodValue
                : normalizeOptionalPeriodValue(parsedPeriodType, periodValue, projectId);
        return page(evaluatedUserId, List.of(), projectId, parsedPeriodType, normalizedPeriodValue, null, pageNo,
                pageSize);
    }

    private PageVO<PerformanceRecordVO> page(
            Long evaluatedUserId,
            List<Long> evaluatedUserIds,
            Long projectId,
            PerformancePeriodType periodType,
            String periodValue,
            Boolean current,
            long pageNo,
            long pageSize
    ) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize);
        IPage<PerformanceRecord> page = performanceRecordMapper.selectPage(
                new Page<>(pageQuery.pageNo(), pageQuery.pageSize()),
                Wrappers.<PerformanceRecord>query()
                        .eq(evaluatedUserId != null, "evaluated_user_id", evaluatedUserId)
                        .in(evaluatedUserId == null && !evaluatedUserIds.isEmpty(),
                                "evaluated_user_id", evaluatedUserIds)
                        .eq(projectId != null, "project_id", projectId)
                        .eq(periodType != null, "period_type", periodType == null ? null : periodType.name())
                        .eq(StringUtils.isNotBlank(periodValue), "period_value", periodValue)
                        .eq(current != null, "is_current", current)
                        .orderByDesc("effective_at", "id"));
        List<PerformanceRecordVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(records, page.getTotal(), pageQuery.pageNo(), pageQuery.pageSize());
    }

    private List<Long> normalizeUserIds(List<Long> evaluatedUserIds) {
        if (evaluatedUserIds == null || evaluatedUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        return evaluatedUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private PerformanceRecord buildRecord(
            Long evaluatedUserId,
            Long projectId,
            String periodType,
            String periodValue,
            String grade,
            String comment,
            String modificationReason
    ) {
        PerformanceRecord record = new PerformanceRecord();
        record.setEvaluatorUserId(SecurityUtils.currentUser().id());
        record.setEvaluatedUserId(evaluatedUserId);
        record.setProjectId(projectId);
        record.setPeriodType(periodType);
        record.setPeriodValue(periodValue);
        record.setGrade(grade);
        record.setComment(comment);
        record.setCurrent(true);
        record.setModificationReason(modificationReason);
        record.setEffectiveAt(LocalDateTime.now());
        return record;
    }

    private void insertCurrentRecord(PerformanceRecord record) {
        try {
            performanceRecordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            // 数据库唯一索引作为并发兜底，防止分布式锁异常时出现多条当前有效绩效。
            throw new BizException(ErrorCode.BUSINESS_ERROR,
                    "同一人员同一项目同一周期只能有一条当前有效绩效记录");
        }
    }

    private PerformanceRecord findCurrent(Long evaluatedUserId, Long projectId, String periodType, String periodValue) {
        return performanceRecordMapper.selectOne(Wrappers.<PerformanceRecord>lambdaQuery()
                .eq(PerformanceRecord::getEvaluatedUserId, evaluatedUserId)
                .eq(PerformanceRecord::getProjectId, projectId)
                .eq(PerformanceRecord::getPeriodType, periodType)
                .eq(PerformanceRecord::getPeriodValue, periodValue)
                .eq(PerformanceRecord::getCurrent, true)
                .last("LIMIT 1"));
    }

    private PerformanceRecord requiredPerformance(Long performanceId) {
        PerformanceRecord record = performanceRecordMapper.selectById(performanceId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "绩效记录不存在");
        }
        return record;
    }

    private SysUser requiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "被评价用户不存在");
        }
        return user;
    }

    private SysUser requiredOutsourcerUser(Long userId) {
        SysUser user = requiredUser(userId);
        if (!isOutsourcer(user.getId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "绩效被评定人只能是测试外包人员");
        }
        return user;
    }

    private boolean isOutsourcer(Long userId) {
        return rbacService.listRoleCodesByUserId(userId).contains(RbacService.DEFAULT_OUTSOURCER_ROLE);
    }

    private Project requiredProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        return project;
    }

    private PerformancePeriodType parsePeriodType(String value) {
        try {
            return PerformancePeriodType.valueOf(value.toUpperCase());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.PARAM_ERROR, "绩效周期类型只能是 MONTH、QUARTER 或 PROJECT");
        }
    }

    private PerformanceGrade parseGrade(String value) {
        try {
            return PerformanceGrade.valueOf(value.toUpperCase());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.PARAM_ERROR, "绩效等级只能是 A、B、C");
        }
    }

    private String normalizePeriodValue(PerformancePeriodType periodType, String periodValue, Long projectId) {
        if (periodType == PerformancePeriodType.PROJECT) {
            if (projectId == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "项目周期绩效必须指定项目");
            }
            // 项目周期没有自然年月值，用项目 ID 生成稳定周期值，便于唯一约束和查询复用。
            return "PROJECT-" + projectId;
        }
        if (StringUtils.isBlank(periodValue)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "绩效周期值不能为空");
        }
        String normalized = periodValue.toUpperCase();
        if (periodType == PerformancePeriodType.MONTH && !MONTH_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "月度绩效周期值格式必须是 yyyy-MM");
        }
        if (periodType == PerformancePeriodType.QUARTER && !QUARTER_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "季度绩效周期值格式必须是 yyyy-Qn");
        }
        return normalized;
    }

    private String normalizeOptionalPeriodValue(PerformancePeriodType periodType, String periodValue, Long projectId) {
        if (periodType == PerformancePeriodType.PROJECT && StringUtils.isBlank(periodValue)) {
            return projectId == null ? null : "PROJECT-" + projectId;
        }
        if (StringUtils.isBlank(periodValue)) {
            return null;
        }
        return normalizePeriodValue(periodType, periodValue, projectId);
    }

    private String lockKey(Long evaluatedUserId, Long projectId, String periodType, String periodValue) {
        return "pta:performance:lock:%d:%d:%s:%s".formatted(evaluatedUserId, projectId, periodType, periodValue);
    }

    private PerformanceRecordVO toVO(PerformanceRecord record) {
        SysUser evaluator = sysUserMapper.selectById(record.getEvaluatorUserId());
        SysUser evaluatedUser = sysUserMapper.selectById(record.getEvaluatedUserId());
        Project project = projectMapper.selectById(record.getProjectId());
        return new PerformanceRecordVO(
                record.getId(),
                record.getEvaluatorUserId(),
                evaluator == null ? null : evaluator.getUsername(),
                record.getEvaluatedUserId(),
                evaluatedUser == null ? null : evaluatedUser.getUsername(),
                evaluatedUser == null ? null : evaluatedUser.getRealName(),
                record.getProjectId(),
                project == null ? null : project.getProjectName(),
                record.getPeriodType(),
                record.getPeriodValue(),
                record.getGrade(),
                record.getComment(),
                record.getCurrent(),
                record.getModificationReason(),
                record.getEffectiveAt(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}

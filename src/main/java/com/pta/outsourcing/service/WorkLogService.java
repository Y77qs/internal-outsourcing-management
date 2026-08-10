package com.pta.outsourcing.service;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.WorkLogCreateRequest;
import com.pta.outsourcing.dto.WorkLogUpdateRequest;
import com.pta.outsourcing.vo.WorkLogVO;
import java.time.LocalDate;

public interface WorkLogService {

    /**
     * 当前外包人员提交个人工作日志。
     *
     * @param request 工作日志内容和所属项目。
     * @return 新建后的工作日志。
     */
    WorkLogVO create(WorkLogCreateRequest request);

    /**
     * 当前外包人员修改本人已有工作日志。
     *
     * @param workLogId 工作日志 ID。
     * @param request 修改后的日志内容。
     * @return 修改后的工作日志。
     */
    WorkLogVO update(Long workLogId, WorkLogUpdateRequest request);

    /**
     * 当前外包人员分页查询本人工作日志。
     *
     * @param startDate 开始日期，可为空。
     * @param endDate 结束日期，可为空。
     * @param projectId 项目 ID，可为空。
     * @param pageNo 页码，小于 1 时归一为 1。
     * @param pageSize 每页数量，最大 100。
     * @return 本人工作日志分页数据。
     */
    PageVO<WorkLogVO> pageMine(LocalDate startDate, LocalDate endDate, Long projectId, long pageNo, long pageSize);

    /**
     * 领导或管理员分页查询全部工作日志。
     *
     * @param userId 提交人 ID，可为空。
     * @param projectId 项目 ID，可为空。
     * @param startDate 开始日期，可为空。
     * @param endDate 结束日期，可为空。
     * @param pageNo 页码，小于 1 时归一为 1。
     * @param pageSize 每页数量，最大 100。
     * @return 工作日志分页数据。
     */
    PageVO<WorkLogVO> pageAll(
            Long userId,
            Long projectId,
            LocalDate startDate,
            LocalDate endDate,
            long pageNo,
            long pageSize
    );
}

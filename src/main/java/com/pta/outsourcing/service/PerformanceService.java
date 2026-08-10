package com.pta.outsourcing.service;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.PerformanceCreateRequest;
import com.pta.outsourcing.dto.PerformanceUpdateRequest;
import com.pta.outsourcing.vo.PerformanceRecordVO;
import com.pta.outsourcing.vo.PerformanceUserOptionVO;
import java.util.List;

public interface PerformanceService {

    /**
     * 领导或管理员为外包人员新增当前有效绩效。
     *
     * @param request 绩效被评定人、项目、周期、等级和评价。
     * @return 新增后的当前有效绩效。
     */
    PerformanceRecordVO create(PerformanceCreateRequest request);

    /**
     * 领导或管理员修改当前有效绩效，并保留历史版本。
     *
     * @param performanceId 当前有效绩效记录 ID。
     * @param request 新等级、评价和必填修改原因。
     * @return 修改后生成的新当前绩效。
     */
    PerformanceRecordVO update(Long performanceId, PerformanceUpdateRequest request);

    /**
     * 领导或管理员分页查询绩效记录。
     *
     * @param evaluatedUserId 单个被评定人 ID，优先级高于 evaluatedUserIds。
     * @param evaluatedUserIds 被评定人 ID 集合，可为空。
     * @param projectId 项目 ID，可为空。
     * @param periodType 周期类型，可为空。
     * @param periodValue 周期值，可为空。
     * @param current 是否只看当前有效记录，可为空。
     * @param pageNo 页码，小于 1 时归一为 1。
     * @param pageSize 每页数量，最大 100。
     * @return 绩效分页数据。
     */
    PageVO<PerformanceRecordVO> pageRecords(
            Long evaluatedUserId,
            List<Long> evaluatedUserIds,
            Long projectId,
            String periodType,
            String periodValue,
            Boolean current,
            long pageNo,
            long pageSize
    );

    /**
     * 搜索可被评定绩效的外包人员候选项。
     *
     * @param name 真实姓名模糊查询条件，可为空。
     * @param userId 用户 ID 精确查询条件，可为空。
     * @return 最多 20 条外包人员候选项。
     */
    List<PerformanceUserOptionVO> searchUserOptions(String name, Long userId);

    /**
     * 查询绩效详情。
     *
     * @param performanceId 绩效记录 ID。
     * @return 绩效详情。
     */
    PerformanceRecordVO detail(Long performanceId);

    /**
     * 当前外包人员分页查询本人绩效。
     *
     * @param projectId 项目 ID，可为空。
     * @param current 是否只看当前有效记录，可为空。
     * @param pageNo 页码，小于 1 时归一为 1。
     * @param pageSize 每页数量，最大 100。
     * @return 本人绩效分页数据。
     */
    PageVO<PerformanceRecordVO> pageMine(Long projectId, Boolean current, long pageNo, long pageSize);

    /**
     * 领导或管理员查询指定外包人员绩效历史。
     *
     * @param evaluatedUserId 被评定人 ID。
     * @param projectId 项目 ID，可为空。
     * @param periodType 周期类型，可为空。
     * @param periodValue 周期值，可为空。
     * @param pageNo 页码，小于 1 时归一为 1。
     * @param pageSize 每页数量，最大 100。
     * @return 绩效历史分页数据。
     */
    PageVO<PerformanceRecordVO> history(
            Long evaluatedUserId,
            Long projectId,
            String periodType,
            String periodValue,
            long pageNo,
            long pageSize
    );
}

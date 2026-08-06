package com.pta.outsourcing.service;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.enums.OperationResult;
import com.pta.outsourcing.vo.OperationLogVO;
import java.time.LocalDateTime;

public interface OperationLogService {

    /**
     * 写入操作日志。
     *
     * @param operatorId 操作人 ID。
     * @param operatorName 操作人名称。
     * @param moduleName 模块名称。
     * @param operationType 操作类型。
     * @param requestPath 请求路径。
     * @param requestParams 请求参数。
     * @param result 操作结果。
     * @param errorMessage 错误信息。
     */
    void record(
            Long operatorId,
            String operatorName,
            String moduleName,
            String operationType,
            String requestPath,
            String requestParams,
            OperationResult result,
            String errorMessage
    );

    /**
     * 分页查询操作日志。
     *
     * @param operatorId 操作人 ID，可为空。
     * @param moduleName 模块名称，可为空。
     * @param startTime 开始时间，可为空。
     * @param endTime 结束时间，可为空。
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 操作日志分页数据。
     */
    PageVO<OperationLogVO> pageLogs(
            Long operatorId,
            String moduleName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long pageNo,
            long pageSize
    );
}

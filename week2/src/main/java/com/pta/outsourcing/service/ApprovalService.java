package com.pta.outsourcing.service;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.ApprovalBatchRequest;
import com.pta.outsourcing.dto.ApprovalRequest;
import com.pta.outsourcing.vo.ApplicationVO;
import java.util.List;

public interface ApprovalService {

    /**
     * 分页查询待审批申请。
     *
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 待审批申请分页数据。
     */
    PageVO<ApplicationVO> pagePending(long pageNo, long pageSize);

    /**
     * 审批通过上岗申请。
     *
     * @param applicationId 上岗申请 ID。
     * @param request 审批意见请求。
     * @return 审批后的申请详情。
     */
    ApplicationVO approve(Long applicationId, ApprovalRequest request);

    /**
     * 驳回上岗申请。
     *
     * @param applicationId 上岗申请 ID。
     * @param request 驳回意见请求。
     * @return 驳回后的申请详情。
     */
    ApplicationVO reject(Long applicationId, ApprovalRequest request);

    /**
     * 批量审批或批量驳回申请。
     *
     * @param request 批量审批请求。
     * @return 批量处理后的申请详情列表。
     */
    List<ApplicationVO> batchProcess(ApprovalBatchRequest request);
}

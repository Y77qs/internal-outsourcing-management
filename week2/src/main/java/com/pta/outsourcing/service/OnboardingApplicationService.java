package com.pta.outsourcing.service;

import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.dto.OnboardingApplicationCreateRequest;
import com.pta.outsourcing.vo.ApplicationVO;

public interface OnboardingApplicationService {

    /**
     * 创建上岗申请。
     *
     * @param request 上岗申请请求。
     * @return 创建后的申请详情。
     */
    ApplicationVO create(OnboardingApplicationCreateRequest request);

    /**
     * 分页查询当前用户的上岗申请。
     *
     * @param pageNo 页码。
     * @param pageSize 每页记录数。
     * @return 当前用户申请分页数据。
     */
    PageVO<ApplicationVO> pageMine(long pageNo, long pageSize);

    /**
     * 查询上岗申请详情。
     *
     * @param applicationId 上岗申请 ID。
     * @return 申请详情。
     */
    ApplicationVO detail(Long applicationId);

    /**
     * 撤回当前用户的待审批申请。
     *
     * @param applicationId 上岗申请 ID。
     * @return 撤回后的申请详情。
     */
    ApplicationVO withdraw(Long applicationId);
}

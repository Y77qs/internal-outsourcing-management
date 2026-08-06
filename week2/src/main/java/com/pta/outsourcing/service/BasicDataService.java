package com.pta.outsourcing.service;

import com.pta.outsourcing.vo.DepartmentOptionVO;
import com.pta.outsourcing.vo.ProjectOptionVO;
import java.util.List;

public interface BasicDataService {

    /**
     * 查询启用的部门下拉选项。
     *
     * @return 部门选项列表。
     */
    List<DepartmentOptionVO> listDepartments();

    /**
     * 查询启用的项目下拉选项。
     *
     * @param departmentId 部门 ID，可为空；为空时查询全部启用项目。
     * @return 项目选项列表。
     */
    List<ProjectOptionVO> listProjects(Long departmentId);
}

package com.pta.outsourcing.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysDepartment;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.service.BasicDataService;
import com.pta.outsourcing.vo.DepartmentOptionVO;
import com.pta.outsourcing.vo.ProjectOptionVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BasicDataServiceImpl implements BasicDataService {

    private static final String ENABLED = "ENABLED";

    private final SysDepartmentMapper sysDepartmentMapper;
    private final ProjectMapper projectMapper;

    @Override
    public List<DepartmentOptionVO> listDepartments() {
        return sysDepartmentMapper.selectList(Wrappers.<SysDepartment>lambdaQuery()
                        .eq(SysDepartment::getStatus, ENABLED)
                        .orderByAsc(SysDepartment::getSortOrder, SysDepartment::getId))
                .stream()
                .map(department -> new DepartmentOptionVO(
                        department.getId(),
                        department.getDepartmentCode(),
                        department.getDepartmentName(),
                        department.getParentId(),
                        department.getStatus()
                ))
                .toList();
    }

    @Override
    public List<ProjectOptionVO> listProjects(Long departmentId) {
        return projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                        .eq(Project::getStatus, ENABLED)
                        .eq(departmentId != null, Project::getDepartmentId, departmentId)
                        .orderByDesc(Project::getStartDate)
                        .orderByAsc(Project::getId))
                .stream()
                .map(project -> new ProjectOptionVO(
                        project.getId(),
                        project.getDepartmentId(),
                        project.getProjectCode(),
                        project.getProjectName(),
                        project.getStartDate(),
                        project.getEndDate(),
                        project.getStatus()
                ))
                .toList();
    }
}

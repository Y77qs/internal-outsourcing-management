package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pta.outsourcing.entity.Project;
import com.pta.outsourcing.entity.SysDepartment;
import com.pta.outsourcing.mapper.ProjectMapper;
import com.pta.outsourcing.mapper.SysDepartmentMapper;
import com.pta.outsourcing.service.impl.BasicDataServiceImpl;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BasicDataServiceImplTest {

    private SysDepartmentMapper sysDepartmentMapper;
    private ProjectMapper projectMapper;
    private BasicDataService basicDataService;

    @BeforeEach
    void setUp() {
        sysDepartmentMapper = mock(SysDepartmentMapper.class);
        projectMapper = mock(ProjectMapper.class);
        basicDataService = new BasicDataServiceImpl(sysDepartmentMapper, projectMapper);
    }

    @Test
    void shouldListDepartmentOptions() {
        SysDepartment department = new SysDepartment();
        department.setId(2L);
        department.setParentId(1L);
        department.setDepartmentCode("TEST_PLATFORM");
        department.setDepartmentName("测试平台部");
        department.setStatus("ENABLED");
        when(sysDepartmentMapper.selectList(any())).thenReturn(List.of(department));

        var departments = basicDataService.listDepartments();

        assertThat(departments).hasSize(1);
        assertThat(departments.getFirst().departmentName()).isEqualTo("测试平台部");
    }

    @Test
    void shouldListProjectOptions() {
        Project project = new Project();
        project.setId(1L);
        project.setDepartmentId(2L);
        project.setProjectCode("PTA-OUTSOURCING");
        project.setProjectName("内部测试外包人员管理系统");
        project.setStartDate(LocalDate.of(2026, 8, 3));
        project.setEndDate(LocalDate.of(2026, 8, 7));
        project.setStatus("ENABLED");
        when(projectMapper.selectList(any())).thenReturn(List.of(project));

        var projects = basicDataService.listProjects(2L);

        assertThat(projects).hasSize(1);
        assertThat(projects.getFirst().projectName()).isEqualTo("内部测试外包人员管理系统");
    }
}

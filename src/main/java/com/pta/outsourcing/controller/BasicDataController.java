package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.service.BasicDataService;
import com.pta.outsourcing.vo.DepartmentOptionVO;
import com.pta.outsourcing.vo.ProjectOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "基础资料")
@RestController
@RequiredArgsConstructor
public class BasicDataController {

    private final BasicDataService basicDataService;

    /**
     * 查询启用部门列表，用于前端下拉选择。
     *
     * @return 启用部门选项列表。
     */
    @Operation(summary = "查询部门下拉选项")
    @OperationLog(moduleName = "基础资料", operationType = "查询部门")
    @GetMapping("/api/departments")
    @PreAuthorize("hasAuthority('basic:read')")
    public ResultVO<List<DepartmentOptionVO>> departments() {
        return ResultVO.success(basicDataService.listDepartments());
    }

    /**
     * 查询启用项目列表，用于前端按部门选择项目。
     *
     * @param departmentId 部门 ID，可为空；为空时返回全部启用项目。
     * @return 启用项目选项列表。
     */
    @Operation(summary = "查询项目下拉选项")
    @OperationLog(moduleName = "基础资料", operationType = "查询项目")
    @GetMapping("/api/projects")
    @PreAuthorize("hasAuthority('basic:read')")
    public ResultVO<List<ProjectOptionVO>> projects(
            @Parameter(description = "部门 ID，可为空；为空时返回全部启用项目", example = "2")
            @RequestParam(required = false) Long departmentId
    ) {
        return ResultVO.success(basicDataService.listProjects(departmentId));
    }
}

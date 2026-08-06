package com.pta.outsourcing.controller;

import com.pta.outsourcing.common.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "健康检查")
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 简单健康检查，用于确认 Spring Boot 基础环境已启动。
     *
     * @return 服务运行状态和服务名称。
     */
    @Operation(summary = "健康检查", description = "确认 Spring Boot 应用是否启动成功")
    @GetMapping("/health")
    public ResultVO<Map<String, String>> health() {
        return ResultVO.success(Map.of("status", "UP", "service", "internal-outsourcing-management"));
    }
}

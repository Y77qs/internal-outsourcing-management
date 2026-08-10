package com.pta.outsourcing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("内部测试外包人员管理系统 API")
                .version("v1")
                .description("内部测试外包人员管理系统接口：认证、RBAC、上岗申请、领导审批、工作日志、绩效管理、MQ 通知和操作审计。"));
    }
}

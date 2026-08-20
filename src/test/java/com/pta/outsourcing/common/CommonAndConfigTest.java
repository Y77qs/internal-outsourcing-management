package com.pta.outsourcing.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pta.outsourcing.config.MybatisPlusConfig;
import com.pta.outsourcing.config.OpenApiConfig;
import com.pta.outsourcing.config.RabbitConfig;
import com.pta.outsourcing.config.SecurityConfig;
import com.pta.outsourcing.security.JwtAuthenticationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;

class CommonAndConfigTest {

    @Test
    void resultVoShouldIncludeSuccessFailureAndTraceId() {
        MDC.put("traceId", "trace-1");
        try {
            ResultVO<List<String>> success = ResultVO.success(List.of("ok"));
            ResultVO<Void> empty = ResultVO.success();
            ResultVO<Void> failure = ResultVO.fail(ErrorCode.PARAM_ERROR, "参数错误");

            assertThat(success.code()).isEqualTo(ErrorCode.SUCCESS.getCode());
            assertThat(success.data()).containsExactly("ok");
            assertThat(success.traceId()).isEqualTo("trace-1");
            assertThat(success.timestamp()).isNotNull();
            assertThat(empty.data()).isNull();
            assertThat(failure.code()).isEqualTo(ErrorCode.PARAM_ERROR.getCode());
            assertThat(failure.message()).isEqualTo("参数错误");
        } finally {
            MDC.clear();
        }
    }

    @Test
    void globalExceptionHandlerShouldMapExpectedErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BindException bindException = new BindException(new LoginForm(), "loginRequest");
        bindException.rejectValue("username", "NotBlank", "用户名不能为空");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        assertThat(handler.handleBizException(new BizException(ErrorCode.NOT_FOUND, "不存在")).getBody().message())
                .isEqualTo("不存在");
        assertThat(handler.handleParamException(bindException).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleParamException(bindException).getBody().message()).contains("username");
        assertThat(handler.handleAccessDenied(new AccessDeniedException("deny")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleException(new RuntimeException("boom"), request).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void configBeansShouldBeConstructible() {
        RabbitConfig rabbitConfig = new RabbitConfig();
        MessageConverter converter = rabbitConfig.jackson2JsonMessageConverter(new ObjectMapper());
        assertThat(rabbitConfig.notificationExchange().getName()).isEqualTo(RabbitConfig.NOTIFICATION_EXCHANGE);
        assertThat(rabbitConfig.notificationDeadLetterExchange().getName()).isEqualTo(RabbitConfig.NOTIFICATION_DLX);
        assertThat(rabbitConfig.notificationQueue().getName()).isEqualTo(RabbitConfig.NOTIFICATION_QUEUE);
        assertThat(rabbitConfig.notificationDeadLetterQueue().getName()).isEqualTo(RabbitConfig.NOTIFICATION_DLQ);
        assertThat(rabbitConfig.notificationBinding().getDestination()).isEqualTo(RabbitConfig.NOTIFICATION_QUEUE);
        assertThat(rabbitConfig.notificationDeadLetterBinding().getDestination())
                .isEqualTo(RabbitConfig.NOTIFICATION_DLQ);
        assertThat(rabbitConfig.rabbitTemplate(mock(ConnectionFactory.class), converter).getMessageConverter())
                .isSameAs(converter);

        assertThat(new OpenApiConfig().openApi().getInfo().getTitle()).contains("内部测试外包人员管理系统");
        assertThat(new MybatisPlusConfig().mybatisPlusInterceptor().getInterceptors()).hasSize(1);
    }

    @Test
    void securityConfigShouldExposePasswordEncoderAndRejectFormLoginUserDetails() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                new ObjectMapper()
        );

        assertThat(securityConfig.passwordEncoder().matches("secret", securityConfig.passwordEncoder().encode("secret")))
                .isTrue();
        assertThatThrownBy(() -> securityConfig.userDetailsService().loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("仅支持 JWT 认证");
    }

    @Test
    void pageVoAndErrorCodeShouldExposeValues() {
        PageVO<String> page = new PageVO<>(List.of("a"), 1, 1, 10);
        assertThat(page.records()).containsExactly("a");
        assertThat(ErrorCode.SUCCESS.getCode()).isEqualTo("00000");
        assertThat(ErrorCode.SYSTEM_ERROR.getMessage()).isNotBlank();
    }

    static class LoginForm {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}

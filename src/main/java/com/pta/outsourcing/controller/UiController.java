package com.pta.outsourcing.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class UiController {

    /**
     * 默认入口跳转到登录页，便于人工验收时直接打开系统。
     *
     * @return 重定向到登录页。
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/ui/login";
    }

    /**
     * 返回登录页面模板。
     *
     * @return `login` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/login")
    public String login() {
        return "login";
    }

    /**
     * 返回测试外包人员注册页面模板。
     *
     * @return `register` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/register")
    public String register() {
        return "register";
    }

    /**
     * 返回系统工作台页面模板。
     *
     * @return `dashboard` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    /**
     * 返回上岗申请页面模板。
     *
     * @return `applications` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/applications")
    public String applications() {
        return "applications";
    }

    /**
     * 返回领导审批页面模板。
     *
     * @return `approvals` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/approvals")
    public String approvals() {
        return "approvals";
    }

    /**
     * 返回工作日志页面模板。
     *
     * @return `work-logs` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/work-logs")
    public String workLogs() {
        return "work-logs";
    }

    /**
     * 返回绩效管理页面模板。
     *
     * @return `performances` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/performances")
    public String performances() {
        return "performances";
    }

    /**
     * 返回用户管理页面模板。
     *
     * @return `users` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/users")
    public String users() {
        return "users";
    }

    /**
     * 返回 MQ 通知消息页面模板。
     *
     * @return `notifications` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/notifications")
    public String notifications() {
        return "notifications";
    }

    /**
     * 返回操作日志审计页面模板。
     *
     * @return `operation-logs` Thymeleaf 模板名称。
     */
    @GetMapping("/ui/operation-logs")
    public String operationLogs() {
        return "operation-logs";
    }
}

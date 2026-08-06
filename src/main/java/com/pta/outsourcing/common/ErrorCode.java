package com.pta.outsourcing.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS("00000", "成功"),
    PARAM_ERROR("A0400", "请求参数错误"),
    UNAUTHORIZED("A0401", "未登录或 Token 无效"),
    FORBIDDEN("A0403", "无访问权限"),
    NOT_FOUND("A0404", "资源不存在"),
    USERNAME_EXISTS("A0409", "用户名已存在"),
    BUSINESS_ERROR("B0001", "业务处理失败"),
    SYSTEM_ERROR("B0500", "系统异常");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}

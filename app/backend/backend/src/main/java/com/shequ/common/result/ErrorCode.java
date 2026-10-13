package com.shequ.common.result;

import lombok.Getter;

/**
 * 错误码表（对应《接口文档》1.5）。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAM_ERROR(40001, "参数错误"),
    SENSITIVE_WORD(40002, "内容命中敏感词，发布被拦截"),
    SMS_CODE_ERROR(40003, "验证码错误或已过期"),
    UNAUTHORIZED(40101, "未登录或登录已失效"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在或已删除"),
    DUPLICATE_OPERATION(40901, "重复操作"),
    SMS_RATE_LIMITED(42901, "触发短信限频"),
    LOGIN_LOCKED(42902, "登录错误次数过多，已锁定"),
    SERVER_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
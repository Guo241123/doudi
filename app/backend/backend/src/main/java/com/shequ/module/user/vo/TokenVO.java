package com.shequ.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 重置密码响应：重置后自动登录（接口文档 #7）。
 */
@Getter
@AllArgsConstructor
public class TokenVO {

    private final String token;

}
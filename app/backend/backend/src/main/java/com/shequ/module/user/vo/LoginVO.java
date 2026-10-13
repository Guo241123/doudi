package com.shequ.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录成功响应（接口文档 #4/#5/#6）。
 */
@Getter
@AllArgsConstructor
public class LoginVO {

    private final String token;

    private final UserVO user;

}
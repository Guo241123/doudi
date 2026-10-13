package com.shequ.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 注册①响应：注册流程凭证（接口文档 #2）。
 */
@Getter
@AllArgsConstructor
public class RegisterTokenVO {

    private final String registerToken;

}
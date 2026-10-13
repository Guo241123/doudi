package com.shequ.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发送短信验证码响应（接口文档 #1）。
 */
@Getter
@AllArgsConstructor
public class SmsSendVO {

    /** 该手机号今日剩余可发条数 */
    private final int leftToday;

}
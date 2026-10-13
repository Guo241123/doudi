package com.shequ.common.captcha;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图形验证码响应。
 */
@Getter
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码会话 ID，提交短信接口时带上 */
    private final String captchaId;

    /** 验证码图片（base64） */
    private final String imageBase64;

}
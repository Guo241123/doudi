package com.shequ.common.captcha;

/**
 * 图形验证码：短信接口的强制前置（避坑清单 Top1——纯接口裸奔必被刷）。
 */
public interface CaptchaService {

    CaptchaVO generate();

    /**
     * 校验并作废。接口文档 #0 规定"一次性"——无论对错，校验后立即失效。
     *
     * @return 是否通过
     */
    boolean verify(String captchaId, String code);

}
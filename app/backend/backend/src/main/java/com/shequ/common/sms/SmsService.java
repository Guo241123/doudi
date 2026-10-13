package com.shequ.common.sms;

/**
 * 短信发送。开发期用 {@link MockSmsService}（验证码只落日志），上线换成真实实现。
 */
public interface SmsService {

    /**
     * 发送短信验证码。
     *
     * @param mobile 手机号
     * @param code   6 位验证码
     */
    void sendCode(String mobile, String code);

}
package com.shequ.common.sms;

import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 生产实现占位：短信签名 / 模板与云厂商 SDK 接入后在此补全（W2 待办）。
 * 未接入前显式报错，避免"以为发了实际没发"。
 */
@Service
@ConditionalOnProperty(name = "sms.mock-enabled", havingValue = "false")
public class AliyunSmsService implements SmsService {

    @Override
    public void sendCode(String mobile, String code) {
        throw new BusinessException(ErrorCode.SERVER_ERROR, "短信服务尚未接入，请先完成签名/模板申请与 SDK 配置");
    }

}
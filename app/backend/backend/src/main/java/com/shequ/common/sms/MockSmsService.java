package com.shequ.common.sms;

import com.shequ.common.util.MaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 开发桩实现：短信签名 / 模板审核要 1～3 天（避坑清单第四节），未下来前不能真发。
 * 验证码写 Redis 由调用方完成，这里只把验证码打到日志，供本地联调自取。
 * 手机号按合规要求脱敏后再打日志。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockSmsService implements SmsService {

    @Override
    public void sendCode(String mobile, String code) {
        log.info("[MockSms] 发送验证码 mock，mobile={}，code={}", MaskUtil.maskMobile(mobile), code);
    }

}
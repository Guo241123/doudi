package com.shequ.common.sms;

import com.shequ.common.constant.RedisKeys;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 短信验证码校验：错误累计 5 次该验证码作废（接口文档 #1 约定）。
 * <p>
 * 注册登录（{@code AuthServiceImpl}）与改手机号 / 改密码（{@code UserServiceImpl}）共用同一套
 * 校验与作废规则，抽到 common 避免各处重复实现。
 */
@Component
@RequiredArgsConstructor
public class SmsCodeVerifier {

    /** 验证码有效期（发送与校验共用同一 TTL） */
    public static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    @Value("${sms.max-verify-fail:5}")
    private int smsMaxVerifyFail;

    /**
     * 校验短信验证码；通过即消费（删除），错误累计到上限则该验证码直接作废。
     *
     * @param scene 短信场景（REGISTER / LOGIN / RESET / CHANGE_MOBILE）
     */
    public void verify(String scene, String mobile, String inputCode) {
        String codeKey = RedisKeys.smsCode(scene, mobile);
        String expected = redis.opsForValue().get(codeKey);
        if (expected == null) {
            throw new BusinessException(ErrorCode.SMS_CODE_ERROR, "验证码已过期，请重新获取");
        }
        if (expected.equals(inputCode)) {
            redis.delete(codeKey);
            redis.delete(RedisKeys.smsError(scene, mobile));
            return;
        }

        String errKey = RedisKeys.smsError(scene, mobile);
        Long errs = redis.opsForValue().increment(errKey);
        if (errs != null && errs == 1L) {
            redis.expire(errKey, CODE_TTL);
        }
        if (errs != null && errs >= smsMaxVerifyFail) {
            redis.delete(codeKey);
            redis.delete(errKey);
            throw new BusinessException(ErrorCode.SMS_CODE_ERROR, "验证码错误次数过多，请重新获取");
        }
        throw new BusinessException(ErrorCode.SMS_CODE_ERROR);
    }

}

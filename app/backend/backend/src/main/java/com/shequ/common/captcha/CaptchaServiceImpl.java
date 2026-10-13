package com.shequ.common.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.shequ.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 图形验证码实现：算式/线条码 + Redis 存答案，2 分钟有效。
 */
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaVO generate() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(RedisKeys.captcha(captchaId), captcha.getCode(), TTL);
        return new CaptchaVO(captchaId, captcha.getImageBase64Data());
    }

    @Override
    public boolean verify(String captchaId, String code) {
        if (captchaId == null || captchaId.isEmpty() || code == null || code.isEmpty()) {
            return false;
        }
        String key = RedisKeys.captcha(captchaId);
        String expected = redisTemplate.opsForValue().get(key);
        // 一次性：无论对错都作废
        redisTemplate.delete(key);
        return expected != null && expected.equalsIgnoreCase(code.trim());
    }

}
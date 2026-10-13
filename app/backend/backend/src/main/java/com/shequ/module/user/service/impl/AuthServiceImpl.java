package com.shequ.module.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.shequ.common.auth.TokenService;
import com.shequ.common.captcha.CaptchaService;
import com.shequ.common.constant.RedisKeys;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import com.shequ.common.sms.SmsCodeVerifier;
import com.shequ.common.sms.SmsService;
import com.shequ.common.storage.FileUrlResolver;
import com.shequ.module.user.dto.DeactivateDTO;
import com.shequ.module.user.dto.LoginPasswordDTO;
import com.shequ.module.user.dto.LoginSmsDTO;
import com.shequ.module.user.dto.PasswordResetDTO;
import com.shequ.module.user.dto.RegisterCheckMobileDTO;
import com.shequ.module.user.dto.RegisterNicknameDTO;
import com.shequ.module.user.dto.RegisterPasswordDTO;
import com.shequ.module.user.dto.SmsSendDTO;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.entity.UserStatus;
import com.shequ.module.user.service.AuthService;
import com.shequ.module.user.service.UserService;
import com.shequ.module.user.vo.LoginVO;
import com.shequ.module.user.vo.RegisterTokenVO;
import com.shequ.module.user.vo.SmsSendVO;
import com.shequ.module.user.vo.TokenVO;
import com.shequ.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 注册登录与账号实现。
 * <p>
 * 关键约束（均来自《接口文档》与《开发避坑清单》）：
 * 1. 短信必须图形验证码前置 + 三重限频（手机号分钟/手机号每日/IP 每日）；
 * 2. 验证码校验错误累计 5 次即作废，须重新发送；
 * 3. 注册中间态只存 Redis，注册③才落库；
 * 4. 密码登录同号连错 5 次锁 15 分钟（同 IP 另做限频）；
 * 5. 注销 / 重置密码等执行后立即清除该用户全部 token。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String SCENE_REGISTER = "REGISTER";
    private static final String SCENE_LOGIN = "LOGIN";
    private static final String SCENE_RESET = "RESET";
    private static final String SCENE_CHANGE_MOBILE = "CHANGE_MOBILE";

    private static final String FIELD_MOBILE = "mobile";
    private static final String FIELD_PASSWORD_HASH = "passwordHash";

    private static final Duration REGISTER_TTL = Duration.ofMinutes(10);
    private static final Duration DEACTIVATE_PERIOD = Duration.ofDays(7);

    private final UserService userService;
    private final TokenService tokenService;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final SmsCodeVerifier smsCodeVerifier;
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;
    private final FileUrlResolver fileUrlResolver;

    @Value("${sms.daily-limit:5}")
    private int smsDailyLimit;
    @Value("${sms.ip-daily-limit:20}")
    private int smsIpDailyLimit;
    @Value("${auth.login-max-fail:5}")
    private int loginMaxFail;
    @Value("${auth.login-lock-minutes:15}")
    private int loginLockMinutes;
    @Value("${auth.login-ip-hourly-limit:30}")
    private int loginIpHourlyLimit;

    @Override
    public SmsSendVO sendSmsCode(SmsSendDTO dto, String ip) {
        // 图形验证码前置（一次性，无论对错作废）
        if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BusinessException(ErrorCode.SMS_CODE_ERROR, "图形验证码错误或已过期");
        }

        String mobile = dto.getMobile();
        String scene = dto.getScene();
        validateScene(mobile, scene);
        int leftToday = consumeSmsQuota(mobile, ip);

        String code = RandomUtil.randomNumbers(6);
        redis.opsForValue().set(RedisKeys.smsCode(scene, mobile), code, SmsCodeVerifier.CODE_TTL);
        redis.delete(RedisKeys.smsError(scene, mobile));
        smsService.sendCode(mobile, code);

        return new SmsSendVO(leftToday);
    }

    @Override
    public RegisterTokenVO registerCheckMobile(RegisterCheckMobileDTO dto) {
        if (userService.getByMobile(dto.getMobile()) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_OPERATION, "手机号已注册，请直接登录");
        }
        smsCodeVerifier.verify(SCENE_REGISTER, dto.getMobile(), dto.getSmsCode());

        String registerToken = UUID.randomUUID().toString().replace("-", "");
        String key = RedisKeys.register(registerToken);
        redis.opsForHash().put(key, FIELD_MOBILE, dto.getMobile());
        redis.expire(key, REGISTER_TTL);
        return new RegisterTokenVO(registerToken);
    }

    @Override
    public void registerSetPassword(RegisterPasswordDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        readRegisterState(dto.getRegisterToken());
        redis.opsForHash().put(RedisKeys.register(dto.getRegisterToken()),
                FIELD_PASSWORD_HASH, passwordEncoder.encode(dto.getPassword()));
        redis.expire(RedisKeys.register(dto.getRegisterToken()), REGISTER_TTL);
    }

    @Override
    public LoginVO registerSetNickname(RegisterNicknameDTO dto) {
        Map<Object, Object> state = readRegisterState(dto.getRegisterToken());
        String mobile = (String) state.get(FIELD_MOBILE);
        String passwordHash = (String) state.get(FIELD_PASSWORD_HASH);
        if (!StringUtils.hasText(passwordHash)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先设置密码");
        }

        String nickname = dto.getNickname().trim();
        if (nickname.isEmpty() || nickname.length() > 12) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称需 1～12 字");
        }
        // 再次校验，防注册流程期间该手机号被其它请求占用
        if (userService.getByMobile(mobile) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_OPERATION, "手机号已注册，请直接登录");
        }

        User user = new User();
        user.setMobile(mobile);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname);
        user.setGender(StringUtils.hasText(dto.getGender()) ? dto.getGender() : "U");
        user.setAvatarUrl(resolveAvatarUrl(dto.getAvatarKey()));
        user.setStatus(UserStatus.NORMAL.name());
        user.setRegisterTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDeleted(0);
        try {
            userService.save(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_OPERATION, "手机号已注册，请直接登录");
        }

        redis.delete(RedisKeys.register(dto.getRegisterToken()));
        return new LoginVO(tokenService.issueAppToken(user.getId()), UserVO.from(user));
    }

    @Override
    public LoginVO loginByPassword(LoginPasswordDTO dto, String ip) {
        String mobile = dto.getMobile();
        User user = userService.getByMobile(mobile);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在，请先注册");
        }
        assertLoginAllowed(user);

        if (Boolean.TRUE.equals(redis.hasKey(RedisKeys.loginLock(mobile)))) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED, "错误次数过多，请 " + loginLockMinutes + " 分钟后再试");
        }
        guardLoginIp(ip);

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            Long errs = redis.opsForValue().increment(RedisKeys.loginError(mobile));
            if (errs != null && errs == 1L) {
                redis.expire(RedisKeys.loginError(mobile), Duration.ofMinutes(loginLockMinutes));
            }
            if (errs != null && errs >= loginMaxFail) {
                redis.opsForValue().set(RedisKeys.loginLock(mobile), "1", Duration.ofMinutes(loginLockMinutes));
                redis.delete(RedisKeys.loginError(mobile));
                throw new BusinessException(ErrorCode.LOGIN_LOCKED,
                        "错误次数过多，账号已锁定 " + loginLockMinutes + " 分钟");
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号或密码错误");
        }

        redis.delete(RedisKeys.loginError(mobile));
        return new LoginVO(tokenService.issueAppToken(user.getId()), UserVO.from(user));
    }

    @Override
    public LoginVO loginBySms(LoginSmsDTO dto) {
        User user = userService.getByMobile(dto.getMobile());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在，请先注册");
        }
        assertLoginAllowed(user);
        smsCodeVerifier.verify(SCENE_LOGIN, dto.getMobile(), dto.getSmsCode());
        return new LoginVO(tokenService.issueAppToken(user.getId()), UserVO.from(user));
    }

    @Override
    public TokenVO resetPassword(PasswordResetDTO dto) {
        User user = userService.getByMobile(dto.getMobile());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在，请先注册");
        }
        assertLoginAllowed(user);
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        smsCodeVerifier.verify(SCENE_RESET, dto.getMobile(), dto.getSmsCode());

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);

        // 高危操作：先清全部 token，再为当前这次重置签发新 token（实现注记 10）
        tokenService.revokeAll(user.getId());
        return new TokenVO(tokenService.issueAppToken(user.getId()));
    }

    @Override
    public void logout(long userId, String jti) {
        tokenService.revokeToken(userId, jti);
    }

    @Override
    public void deactivate(long userId, DeactivateDTO dto) {
        if (!Boolean.TRUE.equals(dto.getConfirm())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请确认后再提交注销");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        user.setStatus(UserStatus.DEACTIVATING.name());
        user.setDeactivateDeadline(LocalDateTime.now().plusDays(7));
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);

        tokenService.revokeAll(userId);
        redis.opsForValue().set(RedisKeys.banUser(userId), "1", DEACTIVATE_PERIOD);
    }

    // ------------------------------------------------------------------ 私有方法

    /**
     * 按场景校验手机号状态（接口文档 #1 的错误约定）。
     */
    private void validateScene(String mobile, String scene) {
        User existing = userService.getByMobile(mobile);
        switch (scene) {
            case SCENE_REGISTER, SCENE_CHANGE_MOBILE -> {
                if (existing != null) {
                    throw new BusinessException(ErrorCode.DUPLICATE_OPERATION, "手机号已注册，请直接登录");
                }
            }
            case SCENE_LOGIN, SCENE_RESET -> {
                if (existing == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在，请先注册");
                }
                assertLoginAllowed(existing);
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "短信场景不合法");
        }
    }

    /**
     * 封禁 / 注销处理中的账号禁止登录与找回（40301）。
     */
    private void assertLoginAllowed(User user) {
        if (UserStatus.BANNED.name().equals(user.getStatus())) {
            String reason = StringUtils.hasText(user.getBanReason()) ? "：" + user.getBanReason() : "";
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁" + reason);
        }
        if (UserStatus.DEACTIVATING.name().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号注销处理中");
        }
    }

    /**
     * 短信三重限频并计数，返回该手机号今日剩余可发条数。
     */
    private int consumeSmsQuota(String mobile, String ip) {
        String minuteKey = RedisKeys.smsMinuteLimit(mobile);
        if (Boolean.TRUE.equals(redis.hasKey(minuteKey))) {
            throw new BusinessException(ErrorCode.SMS_RATE_LIMITED,
                    "发送过于频繁，请 1 分钟后再试（今日剩余 " + remainingToday(mobile) + " 次）");
        }
        int used = remainingToday(mobile);
        if (used <= 0) {
            throw new BusinessException(ErrorCode.SMS_RATE_LIMITED, "该手机号今日短信次数已用完（今日剩余 0 次）");
        }
        if (usedToday(RedisKeys.smsIpLimit(ip)) >= smsIpDailyLimit) {
            throw new BusinessException(ErrorCode.SMS_RATE_LIMITED, "当前网络今日短信次数已达上限");
        }

        incrementDaily(RedisKeys.smsDailyLimit(mobile));
        incrementDaily(RedisKeys.smsIpLimit(ip));
        redis.opsForValue().set(minuteKey, "1", Duration.ofMinutes(1));
        return used - 1;
    }

    private int remainingToday(String mobile) {
        return Math.max(0, smsDailyLimit - usedToday(RedisKeys.smsDailyLimit(mobile)));
    }

    private int usedToday(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private void incrementDaily(String key) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofDays(1));
        }
    }

    /**
     * 登录按 IP 限频（1 小时窗口，防同 IP 撞库）。
     */
    private void guardLoginIp(String ip) {
        String key = RedisKeys.loginIpLimit(ip);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofHours(1));
        }
        if (count != null && count > loginIpHourlyLimit) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED, "当前网络登录尝试过于频繁，请稍后再试");
        }
    }

    private Map<Object, Object> readRegisterState(String registerToken) {
        if (!StringUtils.hasText(registerToken)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "注册凭证不能为空");
        }
        Map<Object, Object> state = redis.opsForHash().entries(RedisKeys.register(registerToken));
        if (state.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "注册流程已过期，请重新开始注册");
        }
        return state;
    }

    /**
     * 头像 URL 组装：统一走 {@link FileUrlResolver}，未传 avatarKey 时返回 null 由前端用默认头像。
     */
    private String resolveAvatarUrl(String avatarKey) {
        if (!StringUtils.hasText(avatarKey)) {
            return null;
        }
        return fileUrlResolver.resolve(avatarKey);
    }

}
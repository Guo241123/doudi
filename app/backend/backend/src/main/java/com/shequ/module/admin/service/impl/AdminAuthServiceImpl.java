package com.shequ.module.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.auth.TokenService;
import com.shequ.common.constant.RedisKeys;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import com.shequ.module.admin.dto.AdminLoginDTO;
import com.shequ.module.admin.entity.AdminAccount;
import com.shequ.module.admin.mapper.AdminAccountMapper;
import com.shequ.module.admin.service.AdminAuthService;
import com.shequ.module.admin.vo.AdminLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 管理员认证实现。登录防撞库与 App 端同规则（错 5 次锁 15 分钟），
 * 但管理员按 username 记锁，Redis 键独立（admin:login:*）。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminAccountMapper adminAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final StringRedisTemplate redis;

    @Value("${auth.login-max-fail:5}")
    private int loginMaxFail;

    @Value("${auth.login-lock-minutes:15}")
    private int loginLockMinutes;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        String username = dto.getUsername();
        if (Boolean.TRUE.equals(redis.hasKey(RedisKeys.adminLoginLock(username)))) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED,
                    "错误次数过多，请 " + loginLockMinutes + " 分钟后再试");
        }

        AdminAccount admin = adminAccountMapper.selectOne(Wrappers.<AdminAccount>lambdaQuery()
                .eq(AdminAccount::getUsername, username));
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            long errs = redis.opsForValue().increment(RedisKeys.adminLoginError(username));
            if (errs == 1L) {
                redis.expire(RedisKeys.adminLoginError(username), Duration.ofMinutes(loginLockMinutes));
            }
            if (errs >= loginMaxFail) {
                redis.opsForValue().set(RedisKeys.adminLoginLock(username), "1",
                        Duration.ofMinutes(loginLockMinutes));
                redis.delete(RedisKeys.adminLoginError(username));
                throw new BusinessException(ErrorCode.LOGIN_LOCKED,
                        "错误次数过多，账号已锁定 " + loginLockMinutes + " 分钟");
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用");
        }

        redis.delete(RedisKeys.adminLoginError(username));
        String token = tokenService.issueAdminToken(admin.getId());
        return new AdminLoginVO(admin.getId(), admin.getNickname(), token);
    }

    @Override
    public String nicknameOf(Long adminId) {
        if (adminId == null) {
            return null;
        }
        AdminAccount admin = adminAccountMapper.selectById(adminId);
        return admin == null || !StringUtils.hasText(admin.getNickname()) ? "社区管理员" : admin.getNickname();
    }

}

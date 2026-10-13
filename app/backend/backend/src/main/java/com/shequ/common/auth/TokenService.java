package com.shequ.common.auth;

import com.shequ.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 会话（token）管理：JWT 无状态 + Redis 白名单校验 jti。
 * <p>
 * 为什么不用纯黑名单：接口文档要求"退出登录只作废当前 token"，同时"改密码/重置/换绑/注销/封禁
 * 要清掉该用户全部设备的 token"（实现注记 10）。黑名单只能逐条作废，做不到"整批清"，故用白名单集合。
 */
@Component
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * 签发 App token 并登记 jti 白名单。
     */
    public String issueAppToken(long userId) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String token = jwtUtil.createToken(userId, JwtUtil.CLIENT_APP, jti);

        String key = RedisKeys.userTokens(userId);
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, Duration.ofSeconds(jwtUtil.appTokenTtlSeconds()));
        return token;
    }

    /**
     * 签发管理员 token（2 小时）并登记独立白名单。
     */
    public String issueAdminToken(long adminId) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String token = jwtUtil.createToken(adminId, JwtUtil.CLIENT_ADMIN, jti);

        String key = RedisKeys.adminTokens(adminId);
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, Duration.ofSeconds(jwtUtil.adminTokenTtlSeconds()));
        return token;
    }

    /**
     * 退出登录：只作废当前这一个 token。
     */
    public void revokeToken(long userId, String jti) {
        if (jti != null) {
            redisTemplate.opsForSet().remove(RedisKeys.userTokens(userId), jti);
        }
    }

    /**
     * 高危操作后清除该用户全部设备 token（含当前会话）。
     */
    public void revokeAll(long userId) {
        redisTemplate.delete(RedisKeys.userTokens(userId));
    }

    /**
     * 校验 token 是否仍在白名单（未被登出 / 未被整批清除）。
     */
    public boolean isValid(long userId, String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisKeys.userTokens(userId), jti));
    }

    /**
     * 校验管理员 token 白名单。
     */
    public boolean isValidAdmin(long adminId, String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisKeys.adminTokens(adminId), jti));
    }

}
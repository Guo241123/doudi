package com.shequ.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验（ADR-005：App 30 天 / 后台 2 小时）。
 * 每个 token 带唯一 jti，配合 Redis 白名单实现"单设备登出"与"整批清 token"。
 */
@Component
public class JwtUtil {

    /** 客户端类型：App 普通用户 */
    public static final String CLIENT_APP = "APP";
    /** 客户端类型：管理后台 */
    public static final String CLIENT_ADMIN = "ADMIN";

    private final SecretKey key;
    private final long appExpireMillis;
    private final long adminExpireMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-days}") long expireDays,
                   @Value("${jwt.admin-expire-hours}") long adminExpireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.appExpireMillis = expireDays * 24 * 60 * 60 * 1000L;
        this.adminExpireMillis = adminExpireHours * 60 * 60 * 1000L;
    }

    /**
     * 签发 token。
     *
     * @param subjectId  用户 ID
     * @param clientType {@link #CLIENT_APP} / {@link #CLIENT_ADMIN}
     * @param jti        token 唯一标识（登出/清 token 靠它定位）
     */
    public String createToken(long subjectId, String clientType, String jti) {
        long ttl = CLIENT_ADMIN.equals(clientType) ? adminExpireMillis : appExpireMillis;
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(subjectId))
                .claim("client", clientType)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验签名与有效期，失败抛 io.jsonwebtoken.JwtException。
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** App token 有效期（秒），用于 Redis 白名单过期时间对齐 */
    public long appTokenTtlSeconds() {
        return appExpireMillis / 1000;
    }

    /** 管理员 token 有效期（秒），用于 Redis 白名单过期时间对齐 */
    public long adminTokenTtlSeconds() {
        return adminExpireMillis / 1000;
    }

}
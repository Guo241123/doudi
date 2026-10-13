package com.shequ.common.auth;

import com.shequ.common.constant.RedisKeys;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理后台鉴权拦截器：只认 ADMIN 类型的 JWT（2 小时有效）。
 * <p>
 * 为什么单独一个拦截器而不塞进 {@link AuthInterceptor}：App 用户 token 与管理员 token
 * 是两套会话体系（ID 空间、白名单键、有效期都不同）。统一拦 /api/admin/v1/** 并强制
 * client=ADMIN，从根上杜绝"普通用户的 App token 调后台接口"（任务模块审查报告的红线项）。
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN_ID = "adminId";
    public static final String ATTR_JTI = "jti";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录管理后台");
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(header.substring(BEARER_PREFIX.length()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }

        // 关键校验：client 必须是 ADMIN，App 用户 token 一律拒绝
        if (!JwtUtil.CLIENT_ADMIN.equals(claims.get("client", String.class))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理后台");
        }

        long adminId = Long.parseLong(claims.getSubject());
        String jti = claims.getId();
        if (!tokenService.isValidAdmin(adminId, jti)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }

        request.setAttribute(ATTR_ADMIN_ID, adminId);
        request.setAttribute(ATTR_JTI, jti);
        return true;
    }

}

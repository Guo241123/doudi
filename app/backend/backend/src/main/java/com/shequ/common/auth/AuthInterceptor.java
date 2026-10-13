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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 登录拦截器：解析 JWT → Redis 黑名单（封禁/注销即时踢下线）→ Redis 白名单（jti 会话校验）。
 * 校验通过后把 userId / jti 写入请求属性；业务层身份一律取自登录态，接口不存在 userId 入参。
 * <p>
 * 游客可读路径（{@link #GUEST_GET_PATTERNS}）不强制登录：不带 token 直接放行（游客态），
 * 带 token 则照常解析登录态，供列表回填 favorited、详情回填 isMine/canChat 用。
 * 写方法（POST/PUT/DELETE）一律需要登录，避免整段路径免登导致越权。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_JTI = "jti";
    public static final String ATTR_CLIENT = "clientType";

    private static final String BEARER_PREFIX = "Bearer ";

    /** 游客可只读（仅 GET）的路径，与 WebMvcConfig 的完全免登路径互斥。 */
    private static final List<String> GUEST_GET_PATTERNS = List.of(
            "/api/v1/tasks/**",
            "/api/v1/trade/categories",
            "/api/v1/trade/items",
            "/api/v1/trade/items/*",
            "/api/v1/forum/feed",
            "/api/v1/forum/posts/*",
            "/api/v1/forum/posts/*/comments");

    /** 游客可访问（不限方法）的路径：广告曝光/点击与埋点上报（#45 / #46）；上传落盘由 URL 上的 HMAC 签名鉴权（前端直传不带 Authorization）。 */
    private static final List<String> GUEST_ANY_PATTERNS = List.of(
            "/api/v1/ads/**",
            "/api/v1/analytics/**",
            "/api/v1/upload/put");

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            // 无 token：游客只读路径放行，其余需登录
            if (isGuestReadable(request)) {
                return true;
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(header.substring(BEARER_PREFIX.length()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        long userId = Long.parseLong(claims.getSubject());
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.banUser(userId)))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁或注销处理中");
        }
        String jti = claims.getId();
        if (!tokenService.isValid(userId, jti)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_JTI, jti);
        request.setAttribute(ATTR_CLIENT, claims.get("client", String.class));
        return true;
    }

    /** 是否允许游客访问：GET 命中只读白名单，或命中不限方法白名单。 */
    private boolean isGuestReadable(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("GET".equalsIgnoreCase(request.getMethod()) && matchesAny(GUEST_GET_PATTERNS, path)) {
            return true;
        }
        return matchesAny(GUEST_ANY_PATTERNS, path);
    }

    private boolean matchesAny(List<String> patterns, String path) {
        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

}

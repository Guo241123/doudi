package com.shequ.config;

import com.shequ.common.auth.AdminAuthInterceptor;
import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置：CORS + 双拦截器注册。
 * <p>
 * AuthInterceptor 拦 /api/**（排除 /api/admin/**，那是 AdminAuthInterceptor 的地盘），
 * 只排除"完全免登"的路径（健康检查与注册登录前几步）。游客只读路径（任务/交易/论坛
 * 列表与详情等）不放这里，交给 AuthInterceptor 按 HTTP 方法放行——否则 POST /trade/items
 * 这类写接口会连同 GET 一起被免登。
 * <p>
 * AdminAuthInterceptor 只拦 /api/admin/v1/**，强制 ADMIN token（App 用户 token 拒绝），
 * 登录接口本身豁免。两个拦截器职责分离，避免"全放行白名单误伤写接口"复发。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final StorageProperties storageProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 管理后台整体交给 AdminAuthInterceptor（两套会话体系，避免双拦）
                        "/api/admin/**",
                        // 健康检查
                        "/api/v1/ping",
                        // 注册登录：仅这些子路径完全免登（logout 需登录，不放行）
                        "/api/v1/auth/captcha",
                        "/api/v1/auth/sms/send",
                        "/api/v1/auth/register/**",
                        "/api/v1/auth/login/**",
                        "/api/v1/auth/password/reset"
                );

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/v1/**")
                .excludePathPatterns(
                        // 管理员登录本身免 token
                        "/api/admin/v1/auth/login"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * provider=local 时把本地存储根目录暴露为 {@code /files/**} 静态资源。
     * 该路径在 /api/** 之外，因此不受登录拦截器约束（媒体文件本身可公开访问）。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!storageProperties.isLocal()) {
            return;
        }
        String location = Paths.get(storageProperties.getLocalDir())
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace('\\', '/');
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + location + "/");
    }

}
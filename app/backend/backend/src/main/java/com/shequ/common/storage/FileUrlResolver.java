package com.shequ.common.storage;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * fileKey → 可访问 URL 的统一解析器。
 * <p>
 * 原先 trade / forum / task / user 四个 service 各写了一份 {@code resolveUrl}，拼的都是
 * {@code oss.endpoint + fileKey}——OSS 没接入（endpoint 为空）时直接原样返回 key，图片全是坏链。
 * 收敛到这里后：
 * <ul>
 *   <li>已带 http(s) 的完整地址：原样返回（种子数据兼容）</li>
 *   <li>provider=local：返回 {@code {当前请求主机}/files/{fileKey}}，本机与真机都能取到</li>
 *   <li>provider=oss：返回 {@code {oss.endpoint}/{fileKey}}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class FileUrlResolver {

    private static final String FILE_PATH_PREFIX = "/files/";

    private final StorageProperties properties;

    @Value("${oss.endpoint:}")
    private String ossEndpoint;

    /** 解析文件的对外访问地址；空 key 返回 null。 */
    public String resolve(String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            return null;
        }
        if (isAbsolute(fileKey)) {
            return fileKey;
        }
        if (!properties.isLocal()) {
            return StringUtils.hasText(ossEndpoint) ? join(ossEndpoint, fileKey) : fileKey;
        }
        String base = currentBaseUrl();
        // 非请求线程（定时任务等）取不到 Host 时退回原 key，避免拼出半截地址
        return StringUtils.hasText(base) ? base + FILE_PATH_PREFIX + fileKey : fileKey;
    }

    /**
     * 当前请求的 {@code scheme://host[:port]}；取不到请求上下文时返回配置的 publicBaseUrl（可能为空串）。
     * 这样真机通过局域网 IP 访问后端时，反回给 App 的图片地址也指向同一个可达主机。
     */
    public String currentBaseUrl() {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return trimTrailingSlash(properties.getPublicBaseUrl());
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        HttpServletRequest request = attrs.getRequest();
        StringBuilder base = new StringBuilder(request.getScheme())
                .append("://")
                .append(request.getServerName());
        if (!isDefaultPort(request)) {
            base.append(':').append(request.getServerPort());
        }
        return base.toString();
    }

    private boolean isAbsolute(String fileKey) {
        return fileKey.startsWith("http://") || fileKey.startsWith("https://");
    }

    private boolean isDefaultPort(HttpServletRequest request) {
        int port = request.getServerPort();
        return ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
    }

    private String join(String prefix, String fileKey) {
        return trimTrailingSlash(prefix) + "/" + fileKey;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

package com.shequ.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取（短信 / 登录按 IP 限频用）。
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能是 "client, proxy1, proxy2"，取第一个
        int comma = ip.indexOf(',');
        return comma > 0 ? ip.substring(0, comma).trim() : ip;
    }

    private static boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }

}
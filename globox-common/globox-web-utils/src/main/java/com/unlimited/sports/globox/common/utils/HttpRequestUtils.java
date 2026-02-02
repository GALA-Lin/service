package com.unlimited.sports.globox.common.utils;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * HTTP请求工具类
 * 用于获取请求相关信息（IP、User-Agent等）
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
public class HttpRequestUtils {

    private static final String UNKNOWN = "unknown";
    private static final String IPV4_LOOPBACK = "127.0.0.1";
    private static final String IPV6_LOOPBACK_COMPRESSED = "::1";
    private static final String IPV6_LOOPBACK_FULL = "0:0:0:0:0:0:0:1";
    private static final String IPV6_LOOPBACK_ZERO_PADDED = "0000:0000:0000:0000:0000:0000:0000:0001";

    /**
     * 获取客户端真实IP地址
     * @param request HTTP请求对象
     * @return 真实IP地址，获取失败返回 "unknown"
     */
    public static String getRealIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            // X-Forwarded-For可能包含多个IP，取第一个（客户端真实IP）
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index).trim();
            }
            return normalizeIpv4Loopback(ip);
        }

        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return normalizeIpv4Loopback(ip);
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return normalizeIpv4Loopback(ip);
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return normalizeIpv4Loopback(ip);
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return normalizeIpv4Loopback(ip);
        }

        // 直接连接的IP
        ip = request.getRemoteAddr();
        if (!StringUtils.hasText(ip)) {
            return UNKNOWN;
        }
        
        // 统一将IPv6回环地址转换为IPv4格式
        return normalizeIpv4Loopback(ip);
    }

    /**
     * 获取客户端真实IP地址（从RequestContextHolder）
     * 
     * @return 真实IP地址，获取失败返回 "unknown"
     */
    public static String getRealIp() {
        HttpServletRequest request = RequestContextHolder.getRequest();
        return getRealIp(request);
    }

    /**
     * 获取User-Agent
     * 
     * @param request HTTP请求对象
     * @return User-Agent字符串，获取失败返回 "unknown"
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String userAgent = request.getHeader("User-Agent");
        return StringUtils.hasText(userAgent) ? userAgent : UNKNOWN;
    }

    /**
     * 获取User-Agent（从RequestContextHolder）
     * 
     * @return User-Agent字符串，获取失败返回 "unknown"
     */
    public static String getUserAgent() {
        HttpServletRequest request = RequestContextHolder.getRequest();
        return getUserAgent(request);
    }

    /**
     * 将IPv6回环地址统一转换为IPv4格式
     * 
     * @param ip IP地址
     * @return 转换后的IP地址
     */
    private static String normalizeIpv4Loopback(String ip) {
        if (ip == null) {
            return ip;
        }
        
        ip = ip.trim();
        
        if (IPV6_LOOPBACK_COMPRESSED.equals(ip) 
                || IPV6_LOOPBACK_FULL.equalsIgnoreCase(ip)
                || IPV6_LOOPBACK_ZERO_PADDED.equalsIgnoreCase(ip)) {
            return IPV4_LOOPBACK;
        }
        
        return ip;
    }
}

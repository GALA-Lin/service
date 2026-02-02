package com.unlimited.sports.globox.common.utils;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 请求工具类
 *
 * @author beanmak1r
 * @since 2024/11/1 15:14
 */
public class RequestContextHolder {

    /**
     * 获取 request 对象
     *
     * @return request 对象，可能为 null
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            return attributes.getRequest();
        }
        return null;
    }


    /**
     * 通过 key 获取请求头值
     *
     * @param key key
     * @return value，可能为 null
     */
    public static String getHeader(String key) {
        HttpServletRequest request = getRequest();
        if (request != null) {
            return request.getHeader(key);
        }
        return null;
    }


    /**
     * 通过 key 获取请求头值
     *
     * @param key key
     * @return value
     */
    public static Long getLongHeader(String key) {
        String rawVal = getHeader(key);
        if (rawVal != null) {
            return Long.parseLong(rawVal);
        }
        return null;

    }


    /**
     * 通过 key 获取请求头值
     *
     * @param key key
     * @return value
     */
    public static Integer getIntHeader(String key) {
        String rawVal = getHeader(key);
        if (rawVal != null) {
            return Integer.parseInt(rawVal);
        }
        return null;
    }


    /**
     * 获取当前请求 ip
     */
    public static String getCurrentRequestIp() {
        HttpServletRequest request = getRequest();
        if (ObjectUtils.isEmpty(request)) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            // 取第一个
            String first = xff.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

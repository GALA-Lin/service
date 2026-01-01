package com.unlimited.sports.globox.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前的请求
     */
    public static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
             return attributes.getRequest();
        }catch (Exception e) {
            return null;
        }
    }


    /**
     * 从请求头中拿取数据
     */
    public static String getHeaderValue(String headerKey) {
        HttpServletRequest request = getRequest();
        if(request != null) {
            return request.getHeader(headerKey);
        }
        return null;
    }

    /**
     * 从请求头中获取指定类型的数据

     * @param headerKey 请求头的 key
     * @param type 目标类型的 Class 对象
     * @param <T> 目标类型
     * @return 转换后的值，如果获取失败或转换异常则返回 null
     */
    public static <T> T getHeaderValue(String headerKey, Class<T> type) {
        String value = getHeaderValue(headerKey);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            return null;
        }
    }
}

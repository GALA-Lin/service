package com.unlimited.sports.globox.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染工具类
 * 使用 Apache Commons Text 进行高性能模板变量替换
 * 支持{变量}格式的模板变量替换
 */
@Slf4j
public class TemplateRenderer {


    /**
     * 渲染模板，替换{变量}为实际值
     * 使用 Apache Commons Text 的 StringSubstitutor 进行高效替换
     *
     * @param template 模板字符串，如"订单{orderId}已确认"
     * @param variables 变量映射，如{"orderId": "123456"}
     * @return 渲染后的字符串
     */
    public static String render(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        if (variables == null || variables.isEmpty()) {
            log.warn("模板变量为空，直接返回模板: {}", template);
            return template;
        }

        // 将 Map<String, Object> 转换为 Map<String, String>
        Map<String, String> stringVariables = new HashMap<>();
        variables.forEach((key, value) -> {
            if (value != null) {
                stringVariables.put(key, value.toString());
            }
        });

        // 创建自定义 StringLookup，处理缺失变量的情况
        StringLookup customLookup = key -> {
            String value = stringVariables.get(key);
            if (value == null) {
                log.warn("模板变量 {} 未找到，保留占位符", key);
                return "{" + key + "}";  // 保留原始占位符
            }
            return value;
        };

        // 使用 StringSubstitutor 进行替换
        StringSubstitutor substitutor = new StringSubstitutor(customLookup, "{", "}", '\\');
        return substitutor.replace(template);
    }


}

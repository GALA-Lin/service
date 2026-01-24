package com.unlimited.sports.globox.common.utils;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 解析工具：基于方法参数名注入变量
 * - 入参：spel / method / args
 * - 返回：spel 计算后的 String
 */
public final class SpelMethodArgsUtils {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private SpelMethodArgsUtils() {}

    public static String eval(String spel, Method method, Object[] args) {
        if (spel == null || spel.isBlank()) {
            throw new IllegalArgumentException("spel is blank");
        }
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }

        StandardEvaluationContext ctx = new StandardEvaluationContext();

        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        if (paramNames != null) {
            int n = Math.min(paramNames.length, args == null ? 0 : args.length);
            for (int i = 0; i < n; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        Expression exp = PARSER.parseExpression(spel);
        String result = exp.getValue(ctx, String.class);

        if (result == null) {
            throw new IllegalArgumentException("spel result is null, spel=" + spel);
        }
        return result;
    }
}
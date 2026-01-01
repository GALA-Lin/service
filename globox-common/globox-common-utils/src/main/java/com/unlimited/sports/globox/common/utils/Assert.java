package com.unlimited.sports.globox.common.utils;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.ResultCode;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * 断言工具类
 *
 * @author dk
 * @since 2025/12/17 22:06
 */
public class Assert {
    /**
     * 条件为 true
     *
     * @param expression 表达式
     * @param resultCode 不满足条件时的错误码
     */
    public static void isTrue(boolean expression, ResultCode resultCode) {
        if (!expression) {
            throw new GloboxApplicationException(resultCode);
        }
    }


    /**
     * 验证给定的表达式是否为真，如果为假则抛出异常。
     *
     * @param expression 要验证的布尔表达式
     * @param messageSupplier 当表达式为假时提供错误信息的函数
     */
    public static void isTrue(boolean expression, Supplier<ResultCode> messageSupplier) {
        if (!expression) {
            throw new GloboxApplicationException(messageSupplier.get());
        }
    }


    /**
     * 条件为 false
     *
     * @param expression 表达式
     * @param resultCode 不满足条件时的错误码
     */
    public static void isFalse(boolean expression, ResultCode resultCode) {
        if (expression) {
            throw new GloboxApplicationException(resultCode);
        }
    }


    /**
     * 对象不为空
     *
     * @param obj        检查对象
     * @param resultCode 不满足条件时的错误码
     */
    public static void isNotEmpty(Object obj, ResultCode resultCode) {
        if (obj == null) {
            throw new GloboxApplicationException(resultCode);
        } else if (obj instanceof Optional<?> opt) {
            if (opt.isEmpty()) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj instanceof CharSequence charSequence) {
            if (charSequence.isEmpty()) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj.getClass().isArray()) {
            if (Array.getLength(obj) == 0) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                throw new GloboxApplicationException(resultCode);
            }
        }
    }


    /**
     * 对象为空
     *
     * @param obj        检查对象
     * @param resultCode 不满足条件时的错误码
     */
    public static void isEmpty(Object obj, ResultCode resultCode) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Optional<?> opt) {
            if (opt.isPresent()) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj instanceof CharSequence charSequence) {
            if (!charSequence.isEmpty()) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj.getClass().isArray()) {
            if (Array.getLength(obj) != 0) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj instanceof Collection<?> collection) {
            if (!collection.isEmpty()) {
                throw new GloboxApplicationException(resultCode);
            }
        } else if (obj instanceof Map<?, ?> map) {
            if (!map.isEmpty()) {
                throw new GloboxApplicationException(resultCode);
            }
        } else {
            throw new GloboxApplicationException(resultCode);
        }
    }


    /**
     * 检查 Result 是否成功
     *
     * @param r        com.unlimited.sports.globox.common.result.R
     * @param userCode 不满足条件时的错误码
     */
    public static void resultOk(R r, ResultCode userCode) {
        if (!r.success()) {
            throw new GloboxApplicationException(userCode);
        }
    }
}

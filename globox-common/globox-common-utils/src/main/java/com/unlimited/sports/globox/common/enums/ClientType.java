package com.unlimited.sports.globox.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 客户端类型枚举
 */
@Getter
@AllArgsConstructor
public enum ClientType {

    /**
     * 用户原生App
     */
    APP(1, "app"),

    /**
     * 用户小程序
     */
    JSAPI(2, "jsapi"),

    /**
     * 商家小程序
     */
    MERCHANT(3, "merchant"),

    /**
     * 第三方小程序
     */
    THIRD_PARTY_JSAPI(4, "third-party-jsapi");

    @EnumValue
    private final Integer code;

    private final String value;



    /**
     * 根据字符串值获取枚举
     *
     * @param value 字符串值
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static ClientType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}


package com.unlimited.sports.globox.common.enums;

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
    APP("app"),

    /**
     * 用户小程序/H5
     */
    JSAPI("jsapi"),

    /**
     * 商家小程序
     */
    MERCHANT("merchant"),

    /**
     * 第三方小程序
     */
    THIRD_PARTY_JSAPI("third-party-jsapi");

    private final String value;

    /**
     * 根据字符串值获取枚举
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

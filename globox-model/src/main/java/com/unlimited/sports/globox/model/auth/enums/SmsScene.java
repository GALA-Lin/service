package com.unlimited.sports.globox.model.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 短信场景枚举
 */
@Getter
@AllArgsConstructor
public enum SmsScene {
    LOGIN(0, "登录"),
    CANCEL(1, "注销"),
    BIND(2, "绑定手机号");

    private final Integer code;
    private final String description;

    public static SmsScene fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SmsScene scene : values()) {
            if (scene.code.equals(code)) {
                return scene;
            }
        }
        return null;
    }
}

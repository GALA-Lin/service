package com.unlimited.sports.globox.common.enums.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 通知关联实体类型枚举
 * 用于标识通知消息中需要附加展示的关联实体信息
 * 支持的实体类型可逐步扩展
 */
@Getter
@AllArgsConstructor
public enum NotificationEntityTypeEnum {

    /**
     * 无需附加实体信息（默认）
     */
    NONE(0, "none"),

    /**
     * 用户信息（头像、昵称）
     * 适用场景：社交通知、关注通知、点赞通知等
     */
    USER(1, "user");

    /**
     * 实体类型编码
     */
    private final Integer code;

    /**
     * 实体类型标识符（用于前端识别）
     */
    private final String type;

    /**
     * 根据编码获取枚举
     */
    public static NotificationEntityTypeEnum fromCode(Integer code) {
        if (code == null) {
            return NONE;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(NONE);
    }

    /**
     * 根据类型标识符获取枚举
     */
    public static NotificationEntityTypeEnum fromType(String type) {
        if (type == null || type.isEmpty()) {
            return NONE;
        }
        return Arrays.stream(values())
                .filter(e -> e.getType().equals(type))
                .findFirst()
                .orElse(NONE);
    }
}

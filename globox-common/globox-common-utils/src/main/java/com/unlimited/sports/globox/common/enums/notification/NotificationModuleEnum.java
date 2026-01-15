package com.unlimited.sports.globox.common.enums.notification;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知模块枚举（一级分类）
 * 按业务模块划分
 */
@Getter
@AllArgsConstructor
public enum NotificationModuleEnum {

    /**
     * 预约场地模块
     */
    VENUE_BOOKING(1, "VENUE_BOOKING", "预约场地模块"),

    /**
     * 教练预定模块
     */
    COACH_BOOKING(2, "COACH_BOOKING", "教练预定模块"),

    /**
     * 约球模块
     */
    PLAY_MATCHING(3, "PLAY_MATCHING", "约球模块"),

    /**
     * 社交模块
     */
    SOCIAL(4, "SOCIAL", "社交模块"),

    /**
     * 系统模块
     */
    SYSTEM(5, "SYSTEM", "系统模块"),
    ;

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String value;

    private final String description;

    public static NotificationModuleEnum fromCode(String code) {
        for (NotificationModuleEnum module : values()) {
            if (module.value.equals(code)) {
                return module;
            }
        }
        throw new IllegalArgumentException("未知的模块: " + code);
    }
}

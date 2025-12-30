package com.unlimited.sports.globox.model.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知角色枚举（二级分类）
 * 按模块划分，每个角色前缀对应所属模块
 */
@Getter
@AllArgsConstructor
public enum NotificationRoleEnum {

    // ========== VENUE_BOOKING 预约场地模块 ==========
    VENUE_BOOKER(1, "VENUE_BOOKER", "订场者"),
    VENUE_MERCHANT(2, "VENUE_MERCHANT", "场地商家"),

    // ========== COACH_BOOKING 教练预定模块 ==========
    COACH_BOOKER(3, "COACH_BOOKER", "预约者"),
    COACH_PROVIDER(4, "COACH_PROVIDER", "教练"),

    // ========== PLAY_MATCHING 约球模块 ==========
    RALLY_INITIATOR(5, "RALLY_INITIATOR", "发起人"),
    RALLY_PARTICIPANT(6, "RALLY_PARTICIPANT", "参与者"),

    // ========== SOCIAL 社交模块 ==========
    SOCIAL_USER(7, "SOCIAL_USER", "用户"),

    // ========== SYSTEM 系统模块 ==========
    SYSTEM_USER(8, "SYSTEM_USER", "用户");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String value;

    private final String description;

    public static NotificationRoleEnum fromCode(String code) {
        for (NotificationRoleEnum role : values()) {
            if (role.value.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的角色: " + code);
    }
}


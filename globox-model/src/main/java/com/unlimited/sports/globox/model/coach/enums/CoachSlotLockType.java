package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 教练时段锁定类型枚举
 */
@Getter
@AllArgsConstructor
public enum CoachSlotLockType {

    /**
     * 1 - 用户下单锁定（临时锁定，15分钟后自动释放）
     */
    USER_ORDER_LOCK(1, "用户下单锁定"),

    /**
     * 2 - 教练手动锁定（需要教练主动解锁）
     */
    COACH_MANUAL_LOCK(2, "教练手动锁定"),

    /**
     * 3 - 自定义日程锁定（自定义日程创建时自动锁定）
     */
    CUSTOM_SCHEDULE_LOCK(3, "自定义日程锁定");

    private final Integer code;
    private final String description;

}
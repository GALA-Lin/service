package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 教练时段记录状态枚举
 * 统一状态定义，与CoachDubboServiceImpl保持一致
 */
@Getter
@AllArgsConstructor
public enum CoachSlotRecordStatusEnum {

    /**
     * 0 - 可用（通常无需创建记录，无记录即表示可用）
     * 如果有记录且状态为0，也表示可用
     */
    AVAILABLE(0, "可预约"),

    /**
     * 1 - 锁定中（用户下单锁定，15分钟内有效）
     */
    LOCKED(1, "锁定中"),

    /**
     * 2 - 不可预约（教练手动关闭该时段）
     */
    UNAVAILABLE(2, "不可预约"),

    /**
     * 3 - 自定义日程占用（教练创建的自定义日程）
     */
    CUSTOM_EVENT(3, "自定义日程");

    private final Integer code;
    private final String description;

    public static CoachSlotRecordStatusEnum fromCode(Integer code) {
        if (code == null) {
            return AVAILABLE;
        }
        for (CoachSlotRecordStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的状态码: " + code);
    }

    public static String getDescription(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (Exception e) {
            return "未知状态";
        }
    }
}
package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/31 16:51
 * 教练状态枚举
 */
@Getter
@AllArgsConstructor
public enum CoachStatusEnum {
    PAUSED(0, "暂停接单"),
    ACTIVE(1, "正常接单"),
    ON_VACATION(2, "休假中");

    private final Integer code;
    private final String description;

    public static CoachStatusEnum fromValue(Integer code) {
        for (CoachStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的教练状态: " + code);
    }
}

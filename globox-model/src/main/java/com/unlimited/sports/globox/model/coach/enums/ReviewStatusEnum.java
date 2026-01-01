package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2026/1/1 12:57
 * 评价状态枚举
 */
@Getter
@AllArgsConstructor
public enum ReviewStatusEnum {
    HIDDEN(0, "已隐藏"),
    VISIBLE(1, "正常显示");

    private final Integer value;
    private final String description;


    public static ReviewStatusEnum fromValue(Integer value) {
        for (ReviewStatusEnum status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的评价状态: " + value);
    }
}

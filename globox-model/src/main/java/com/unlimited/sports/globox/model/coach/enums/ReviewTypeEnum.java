package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2026/1/1 12:49
 * 评论类型枚举
 */
@Getter
@AllArgsConstructor
public enum ReviewTypeEnum {
    USER_COMMENT(1, "学员评价"),
    COACH_REPLY(2, "教练回复");

    private final Integer value;
    private final String description;

    public static ReviewTypeEnum fromValue(Integer value) {
        for (ReviewTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的评论类型: " + value);
    }
}


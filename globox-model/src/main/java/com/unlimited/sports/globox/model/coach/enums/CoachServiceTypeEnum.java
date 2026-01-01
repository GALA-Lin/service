package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/31 16:48
 * 课程类别枚举类
 */
@Getter
@AllArgsConstructor
public enum CoachServiceTypeEnum {
    ONE_ON_ONE(1, "一对一教学"),
    ONE_ON_THREE(2, "一对一陪练"),
    ONE_ON_TWO(3, "一对二"),
    SMALL_CLASS(4, "小班(3-6人)");

    private final Integer code;
    private final String description;


    public static CoachServiceTypeEnum fromValue(Integer code) {
        for (CoachServiceTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的服务类型: " + code);
    }
}

package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025-12-18-13:36
 * 营业时间规则类型枚举
 */
@Getter
@AllArgsConstructor
public enum BusinessHourRuleTypeEnum {

    REGULAR(1, "常规规则", "按星期重复的营业时间"),
    SPECIAL_DATE(2, "特殊日期", "指定日期的特殊营业时间"),
    CLOSED_DATE(3, "关闭日期", "指定日期关闭，不营业");

    private final Integer code;
    private final String name;
    private final String description;

    public static BusinessHourRuleTypeEnum fromCode(Integer code) {
        for (BusinessHourRuleTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @since 2026/1/2 11:17
 * 教练教龄筛选枚举
 */
@Slf4j
@Getter
@AllArgsConstructor
public enum TeachingYearsFilterEnum {
    LESS_THAN_3_YEARS(1, "小于3年", null, 2),
    BETWEEN_3_AND_5_YEARS(2, "3-5年", 3, 5),
    BETWEEN_5_AND_8_YEARS(3, "5-8年", 5, 8),
    MORE_THAN_8_YEARS(4, "8年以上", 8, null);

    // 对应前端/入参的数字编码
    private final Integer code;
    // 描述
    private final String desc;
    // 最小教龄
    private final Integer minYears;
    // 最大教龄
    private final Integer maxYears;

    /**
     * 根据编码获取枚举
     */
    public static TeachingYearsFilterEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TeachingYearsFilterEnum filter : values()) {
            if (filter.getCode().equals(code)) {
                return filter;
            }
        }
        log.warn("无效的教龄筛选编码: {}", code);
        return null;
    }
}
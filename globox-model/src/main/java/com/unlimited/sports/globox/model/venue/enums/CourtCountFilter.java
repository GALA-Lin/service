package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 场地片数筛选枚举
 * 用于在搜索时根据场地数量进行筛选
 *
 * 区间定义（闭区间，不重叠）：
 * - SMALL: [1, 4] 包含1,2,3,4片（不包含0片场馆）
 * - MEDIUM: [5, 10] 包含5,6,7,8,9,10片
 * - LARGE: [11, ∞] 包含11片及以上
 */
@Getter
@AllArgsConstructor
public enum CourtCountFilter {
    SMALL(1, "4片以内", 1, 4),
    MEDIUM(2, "4-10片", 5, 10),
    LARGE(3, "10片以上", 11, Integer.MAX_VALUE);

    private final int value;
    private final String description;
    private final int minCount;
    private final int maxCount;


    public static CourtCountFilter fromValue(int value) {
        for (CourtCountFilter filter : values()) {
            if (filter.value == value) {
                return filter;
            }
        }
        throw new IllegalArgumentException("Unknown CourtCountFilter value: " + value);
    }
}

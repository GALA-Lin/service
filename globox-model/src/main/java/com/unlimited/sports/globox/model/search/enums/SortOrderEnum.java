package com.unlimited.sports.globox.model.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 排序顺序枚举
 */
@Getter
@AllArgsConstructor
public enum SortOrderEnum {

    /**
     * 升序
     */
    ASC(1, "asc"),

    /**
     * 降序
     */
    DESC(2, "desc");

    /**
     * 前端传入值
     */
    private final Integer value;

    /**
     * ES排序字符串
     */
    private final String sortOrder;


    /**
     * 根据前端传入值获取对应的排序顺序
     *
     * @param value 前端传入值（1=升序, 2=降序）
     * @return 排序顺序枚举，默认降序
     */
    public static SortOrderEnum fromValue(Integer value) {
        return Arrays.stream(values())
                .filter(order -> order.getValue().equals(value))
                .findFirst()
                .orElse(DESC);
    }
}

package com.unlimited.sports.globox.model.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 笔记搜索排序方式枚举
 */
@Getter
@AllArgsConstructor
public enum NoteSortTypeEnum {

    /**
     * 最新 - 按创建时间降序
     */
    LATEST("latest", "最新"),

    /**
     * 最热 - 按热度分数降序
     */
    HOTTEST("hottest", "最热"),

    /**
     * 精选 - 按质量分数降序
     */
    SELECTED("selected", "精选");

    /**
     * 排序方式代码
     */
    private final String code;

    /**
     * 排序方式描述
     */
    private final String description;

    /**
     * 根据代码获取枚举值
     *
     * @param code 排序方式代码
     * @return 对应的枚举值，默认为LATEST
     */
    public static NoteSortTypeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(LATEST);
    }
}

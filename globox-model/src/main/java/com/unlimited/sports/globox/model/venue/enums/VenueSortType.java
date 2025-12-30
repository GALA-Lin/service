package com.unlimited.sports.globox.model.venue.enums;

import lombok.Getter;

/**
 * 场馆搜索排序类型枚举 V2
 * 用于MySQL搜索场景
 */
@Getter
public enum VenueSortType {

    /**
     * 按距离排序（从近到远）
     */
    DISTANCE("distance", "距离排序"),

    /**
     * 按价格排序（从低到高）
     */
    PRICE("price", "价格排序"),

    /**
     * 按场地数量排序（从多到少）
     */
    COURT_COUNT("courtCount", "场地数量排序");

    /**
     * 排序类型代码
     */
    private final String code;

    /**
     * 排序类型描述
     */
    private final String description;

    VenueSortType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 排序类型代码
     * @return 对应的枚举，如果不存在则返回null
     */
    public static VenueSortType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (VenueSortType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 验证排序代码是否有效
     *
     * @param code 排序类型代码
     * @return true如果有效，否则false
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}

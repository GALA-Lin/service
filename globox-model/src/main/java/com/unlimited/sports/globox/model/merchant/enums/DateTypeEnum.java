package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日期类型枚举
 * 用于区分工作日、周末、节假日，以便应用不同的价格策略
 *
 * @author Linsen Hu
 * @since 2025/12/24
 */
@Getter
@AllArgsConstructor
public enum DateTypeEnum {

    /**
     * 工作日（周一到周五）
     */
    WEEKDAY(1, "工作日", "周一到周五"),

    /**
     * 周末（周六、周日）
     */
    WEEKEND(2, "周末", "周六、周日"),

    /**
     * 节假日（法定节假日）
     */
    HOLIDAY(3, "节假日", "法定节假日");

    private final Integer code;
    private final String name;
    private final String description;


    /**
     * 根据日期判断日期类型
     * TODO: 后续需要接入节假日API或维护节假日表
     *
     * @param dayOfWeek 星期几（1=周一，2=周二，...，7=周日）
     * @return 日期类型
     */
    public static DateTypeEnum getDateType(int dayOfWeek) {
        // TODO: 判断是否为节假日，需要接入节假日API或查询节假日表
        // 可以考虑：
        // 1. 接入第三方节假日API（如：国务院办公厅公布的节假日安排）
        // 2. 维护本地节假日表
        // 3. 使用开源的节假日库

        // 目前先简单判断周末
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return WEEKEND;
        }
        return WEEKDAY;
    }

    /**
     * 根据日期判断日期类型（重载方法，接受LocalDate）
     *
     * @param date 日期
     * @return 日期类型
     */
    public static DateTypeEnum getDateType(java.time.LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return getDateType(dayOfWeek);
    }
}
package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VenueActivityStatusEnum {

    NORMAL(1, "正常"),
    CANCELLED(2, "取消"),
    ;

    private final Integer value;

    private final String desc;


    public static VenueActivityStatusEnum fromValue(Integer status) {
        // 1. 遍历枚举的所有常量
        for (VenueActivityStatusEnum enumItem : VenueActivityStatusEnum.values()) {
            // 2. 空值判断 + 匹配value值（避免NullPointerException）
            if (status != null && status.equals(enumItem.getValue())) {
                return enumItem;
            }
        }
        return null;
    }
}

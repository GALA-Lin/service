package com.unlimited.sports.globox.common.enums.governance;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 举报工单状态枚举
 */
@Getter
@AllArgsConstructor
public enum ComplaintStatusEnum {

    /**
     * 待处理
     */
    PENDING(0, "待处理"),

    /**
     * 处理中
     */
    PROCESSING(1, "处理中"),

    /**
     * 成立
     */
    CONFIRMED(2, "成立"),

    /**
     * 不成立
     */
    REJECTED(3, "不成立"),

    /**
     * 撤销/关闭
     */
    CLOSED(4, "撤销/关闭");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String desc;

    public static ComplaintStatusEnum of(Integer code) {
        if (code == null) return null;
        for (ComplaintStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}
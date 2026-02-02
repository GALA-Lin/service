package com.unlimited.sports.globox.model.social.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 约球申请状态枚举
 */
@Getter
@AllArgsConstructor
public enum RallyApplyStatusEnum {
    PENDING(0, "待审核"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已取消"),
    DEFAULT(4, "申请加入");

    private final int code;
    private final String description;


    public static RallyApplyStatusEnum fromCode(int code) {
        for (RallyApplyStatusEnum status : RallyApplyStatusEnum.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
    

}

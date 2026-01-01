package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 申请退款状态枚举
 */
@Getter
@AllArgsConstructor
public enum ApplyRefundStatusEnum {

    PENDING(1, "PENDING"),
    APPROVED(2, "APPROVED"),
    REJECTED(3, "REJECTED"),
    /**
     * 用户手动取消申请
     */
    CANCELLED(4, "CANCELLED"),
    ;


    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}

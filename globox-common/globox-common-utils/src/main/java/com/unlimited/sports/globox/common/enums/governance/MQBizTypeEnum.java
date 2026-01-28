package com.unlimited.sports.globox.common.enums.governance;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.unlimited.sports.globox.common.enums.BusinessServiceEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 队列相关业务类型枚举，标明具体的操作类型
 */
@Getter
@AllArgsConstructor
public enum MQBizTypeEnum {
    UNKNOW(0, BusinessServiceEnum.UNKNOW, BusinessServiceEnum.UNKNOW),
    UNLOCK_SLOT_MERCHANT(1, BusinessServiceEnum.ORDER, BusinessServiceEnum.MERCHANT),
    ORDER_AUTO_CANCEL(2, BusinessServiceEnum.ORDER, BusinessServiceEnum.ORDER),
    ORDER_AUTO_COMPLETE(3, BusinessServiceEnum.ORDER, BusinessServiceEnum.ORDER),
    PAYMENT_REFUND_SUCCESS(4, BusinessServiceEnum.PAYMENT, BusinessServiceEnum.ORDER),
    PAYMENT_SUCCESS(5, BusinessServiceEnum.PAYMENT, BusinessServiceEnum.ORDER),
    ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT(6, BusinessServiceEnum.ORDER, BusinessServiceEnum.MERCHANT),
    USER_REFUND(7, BusinessServiceEnum.ORDER, BusinessServiceEnum.PAYMENT),
    DEVICE_ACTIVATION(8, BusinessServiceEnum.USER, BusinessServiceEnum.NOTIFICATION),
    NOTIFICATION_SYSTEM(9, BusinessServiceEnum.UNKNOW, BusinessServiceEnum.NOTIFICATION),
    NOTIFICATION_CORE(10, BusinessServiceEnum.UNKNOW, BusinessServiceEnum.NOTIFICATION),
    NOTIFICATION_URGENT(11, BusinessServiceEnum.UNKNOW, BusinessServiceEnum.NOTIFICATION),
    UNLOCK_SLOT_COACH(12, BusinessServiceEnum.ORDER, BusinessServiceEnum.COACH),
    PROFIT_SHARING(13, BusinessServiceEnum.ORDER, BusinessServiceEnum.PAYMENT)
    ;

    @EnumValue
    @JsonValue
    private final Integer code;
    private final BusinessServiceEnum fromService;
    private final BusinessServiceEnum toService;

    public static MQBizTypeEnum of(Integer code) {
        if (code == null) return null;
        for (MQBizTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }

    public String getDescription() {
        return "【" +
                fromService.getDesc() +
                "】" +
                " to " +
                "【" +
                toService.getDesc() +
                "】" +
                "type: " +
                name();
    }
}

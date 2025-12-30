package com.unlimited.sports.globox.model.order.vo;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 取消订单返回结果
 */
@Data
@Builder
public class CancelOrderResultVo implements Serializable {

    /**
     * 订单号
     */
    @NotNull
    private Long orderNo;

    /**
     * 当前订单状态
     */
    @NotNull
    private Integer orderStatus;

    /**
     * 状态描述
     */
    @NotNull
    private String orderStatusName;

    /**
     * 取消时间
     */
    @Null
    private LocalDateTime cancelledAt;
}
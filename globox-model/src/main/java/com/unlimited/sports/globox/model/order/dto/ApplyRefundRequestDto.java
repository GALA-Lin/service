package com.unlimited.sports.globox.model.order.dto;

import com.unlimited.sports.globox.common.enums.order.UserRefundReasonEnum;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Data
public class ApplyRefundRequestDto implements Serializable {

    /**
     * 订单号
     */
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    /**
     * 退款的订单项 ID 列表（必填）
     */
    @NotNull(message = "需要退款的订单项不能为空")
    @Size(message = "需要退款的订单项不能为空", min = 1)
    private List<Long> orderItemIds;

    /**
     * 退款原因编码
     * 1=改变主意
     * 2=日程冲突
     * 3=场馆问题
     * 4=重复付款
     * 5=质量问题
     * 6=天气原因
     * 7=个人紧急事件
     * 8=其他
     */
    @NotNull(message = "退款原因不能为空")
    private UserRefundReasonEnum reasonCode;

    /**
     * 退款原因描述（可选）
     */
    @Null
    private String reasonDetail;
}
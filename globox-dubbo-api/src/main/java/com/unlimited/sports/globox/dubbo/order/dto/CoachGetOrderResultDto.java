package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrdersPaymentStatusEnum;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家分页查询订单返回值
 */
@Data
@Builder
public class CoachGetOrderResultDto implements Serializable {
    /**
     * 订单号
     */
    @NotNull
    private Long orderNo;

    /**
     * 用户ID
     */
    @NotNull
    private Long userId;

    /**
     * 教练ID
     */
    @NotNull
    private Long coachId;

    /**
     * 场馆名称
     */
    @NotNull
    private String coachName;

    /**
     * 是否活动订单
     */
    @NotNull
    private boolean isActivity;

    /**
     * 活动类型名称
     */
    private String activityTypeName;

    /**
     * 基础价格
     */
    @NotNull
    private BigDecimal basePrice;

    /**
     * 额外费用总和
     */
    @NotNull
    private BigDecimal extraChargeTotal;

    /**
     * 小计
     */
    @NotNull
    private BigDecimal subtotal;

    /**
     * 折扣金额
     *
     */
    @Null
    private BigDecimal discountAmount;

    /**
     * 最终总价
     */
    @NotNull
    private BigDecimal totalPrice;

    /**
     * 支付状态
     */
    @NotNull
    private OrdersPaymentStatusEnum paymentStatus;

    /**
     * 支付状态名称
     */
    @NotNull
    private String paymentStatusName;

    /**
     * 订单状态
     */
    @NotNull
    private OrderStatusEnum orderStatus;

    /**
     * 订单状态名称
     */
    @NotNull
    private String orderStatusName;

    /**
     * 订单来源
     */
    @NotNull
    private Integer source;

    /**
     * 支付时间
     */
    @Null
    private LocalDateTime paidAt;

    /**
     * 创建时间
     */
    @NotNull
    private LocalDateTime createdAt;

    /**
     * 订单时段列表
     */
    @NotNull
    private List<RecordDto> records;
}

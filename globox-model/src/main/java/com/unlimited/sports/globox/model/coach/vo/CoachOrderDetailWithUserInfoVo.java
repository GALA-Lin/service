package com.unlimited.sports.globox.model.coach.vo;

import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrdersPaymentStatusEnum;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 教练端订单详情展示VO
 * 组合了订单信息与下单用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachOrderDetailWithUserInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // --- 订单基本信息 (同步自 CoachGetOrderResultDto) ---
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
    private List<CoachRecordDto> records;

    private Long refundApplyId;


    /**
     * 下单人基本信息
     */
    private UserInfoVo buyerInfo;

    /**
     * 下单人电话
     */
    private String buyerPhone;
}
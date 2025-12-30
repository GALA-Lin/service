package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

/**
 * 订单明细表
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value ="order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItems extends BaseEntity implements Serializable {

    /**
     * 订单编号
     */
    private Long orderNo;

    /**
     * 订单项类型：1=COURT, 2=COACH
     */
    private SellerTypeEnum itemType;

    /**
     * 资源 ID（场地 ID/教练 ID）
     */
    private Long resourceId;

    /**
     * 资源名称快照
     */
    private String resourceName;

    /**
     * 槽 ID
     */
    private Long slotId;

    /**
     * 预订日期
     */
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;


    /**
     * 单价快照
     */
    private BigDecimal unitPrice;

    /**
     * 单个槽位额外费用（仅包含SLOT_BASED类型的额外费用），其他额外费用见venue_order_extra_charge表
     */
    private BigDecimal extraAmount;

    /**
     * 小计金额
     */
    private BigDecimal subtotal;

    /**
     * 退款状态：0=NONE，1=REFUNDING，2=REFUNDED
     */
    private RefundStatusEnum refundStatus;


    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
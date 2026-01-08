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
     * record ID
     */
    private Long recordId;

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
     * 单个 record 额外费用
     */
    private BigDecimal extraAmount;

    /**
     * 小计金额
     */
    private BigDecimal subtotal;

    /**
     * 退款状态
     */
    private RefundStatusEnum refundStatus;


    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
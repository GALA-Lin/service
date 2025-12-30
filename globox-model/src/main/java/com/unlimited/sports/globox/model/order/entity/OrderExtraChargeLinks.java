package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import lombok.*;

/**
 * 订单项与额外费用的归属关系表
 *
 * @author dk
 * @since 2025/12/23 09:28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value ="order_extra_charge_links")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderExtraChargeLinks extends BaseEntity implements Serializable {
    /**
     * 订单号
     */
    private Long orderNo;

    /**
     * 订单明细ID
     */
    private Long orderItemId;

    /**
     * 订单额外费用ID
     */
    private Long extraChargeId;

    /**
     * 计费方式：
     * 1=SLOT_BASED，
     * 2=ORDER_BASED，
     * 3=PERCENTAGE
     * 冗余
     */
    private ChargeModeEnum chargeMode;

    /**
     * 分摊到该订单项的额外费用金额（退款以此为准）
     */
    private BigDecimal allocatedAmount;


    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
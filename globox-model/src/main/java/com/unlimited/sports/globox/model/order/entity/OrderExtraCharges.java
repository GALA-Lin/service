package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

/**
 * 订单附加费用表
 *
 * @author dk
 * @since 2025/12/23 09:28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value ="order_extra_charges")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderExtraCharges extends BaseEntity implements Serializable {


    private Long orderNo;

    private Long orderItemId;

    /**
     * VENUE：订单费用类型 ID
     */
    private Long chargeTypeId;

    /**
     * 费用名称
     */
    private String chargeName;

    /**
     * 计费方式：1=SLOT_BASED，2=ORDER_BASED，3=PERCENTAGE
     */
    private ChargeModeEnum chargeMode;

    /**
     * 固定值
     */
    private BigDecimal fixedValue;

    /**
     * 该费用最终金额快照
     */
    private BigDecimal chargeAmount;


    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
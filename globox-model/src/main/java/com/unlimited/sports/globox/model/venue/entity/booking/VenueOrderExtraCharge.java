package com.unlimited.sports.globox.model.venue.entity.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("venue_order_extra_charge")
public class VenueOrderExtraCharge {

    @TableId(value = "charge_id", type = IdType.AUTO)
    private Long chargeId;

    private Long orderId;

    private Integer chargeType;

    private String chargeName;

    private Integer chargeMode;

    /**
     * SLOT_BASED模式：单位时段价格
     */
    private BigDecimal slotUnitAmount;

    /**
     * SLOT_BASED模式：时段数量
     */
    private Integer slotCount;


    /**
     * ORDER_BASED模式：固定金额
     */
    private BigDecimal fixedAmount;

    /**
     * PERCENTAGE模式：百分比值（如10表示10%）
     */
    private BigDecimal percentageValue;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

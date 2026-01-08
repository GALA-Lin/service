package com.unlimited.sports.globox.dubbo.merchant.dto;

import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 槽级别额外费用
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExtraQuote implements Serializable {

    /**
     * 当 VENUE 订单时：代表订单额外费用类型 ID（在商家 service 数据库中的 ID）
     */
    private Long chargeTypeId;

    /**
     * 额外费用名称
     */
    private String chargeName;

    /**
     * 额外费用计费模式
     * com.unlimited.sports.globox.common.enums.order.ChargeModeEnum
     */
    private ChargeModeEnum chargeMode;

    /**
     * 固定值：
     * - charge_mode = FIXED：直接表示金额
     * - charge_mode = PERCENTAGE：表示占比份额（0–100），按 100 等份计算
     *   例如：
     *   10  = 10 / 100
     *   25  = 25 / 100
     *   100 = 100 / 100
     */
    private BigDecimal fixedValue;

    /**
     * 额外费用金额
     */
    private BigDecimal amount;
}
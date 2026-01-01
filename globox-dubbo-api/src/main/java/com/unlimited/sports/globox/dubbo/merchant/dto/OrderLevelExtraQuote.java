package com.unlimited.sports.globox.dubbo.merchant.dto;

import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * 订单级别额外费用
 *
 * @author dk
 * @since 2025/12/23 09:30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLevelExtraQuote implements Serializable {

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
     */
    private ChargeModeEnum chargeMode;

    /**
     * 若是固定金额，填金额；若是百分比，填百分比值
     */
    private BigDecimal fixedValue;

    /**
     * 额外费用总金额
     */
    private BigDecimal amount;
}
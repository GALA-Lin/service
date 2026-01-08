package com.unlimited.sports.globox.venue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 价格计算结果
 * 只包含价格相关信息，不包含任何业务记录相关的内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingInfo {

    /**
     * 槽位时间 -> 价格的映射（根据日期类型计算）
     */
    private Map<LocalTime, BigDecimal> slotPrices;

    /**
     * 基础价格总和（所有槽位的价格总和）
     */
    private BigDecimal basePrice;

    /**
     * 订单级额外费用详情列表
     */
    private List<OrderLevelExtraInfo> orderLevelExtras;

    /**
     * 订单级额外费用总和
     */
    private BigDecimal orderLevelExtraAmount;

    /**
     * 订单项级额外费用（按场地ID分组）
     * Key：场地ID，Value：该场地的订单项级额外费用列表
     */
    private Map<Long, List<ItemLevelExtraInfo>> itemLevelExtrasByCourtId;

    /**
     * 总价格（基础价格 + 订单级额外费用 + 订单项级额外费用）
     */
    private BigDecimal totalPrice;

    /**
     * 订单级额外费用信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLevelExtraInfo {
        /**
         * 费用项ID
         */
        private Long chargeTypeId;

        /**
         * 费用项名称
         */
        private String chargeName;

        /**
         * 计费模式：1=FIXED(固定金额)，2=PERCENTAGE(百分比)
         */
        private Integer chargeMode;

        /**
         * 固定值 / 单价
         */
        @NotNull
        private BigDecimal fixedValue;
        /**
         * 费用金额
         */
        private BigDecimal amount;
    }

    /**
     * 订单项级额外费用信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemLevelExtraInfo {
        /**
         * 费用项ID
         */
        private Long chargeTypeId;

        /**
         * 费用项名称
         */
        private String chargeName;

        /**
         * 计费模式：1=FIXED(固定金额)，2=PERCENTAGE(百分比)
         */
        private Integer chargeMode;

        /**
         * 单位金额或比例
         */
        private BigDecimal fixedValue;

        /**
         * 费用金额（单个槽位）
         */
        private BigDecimal amount;
    }
}

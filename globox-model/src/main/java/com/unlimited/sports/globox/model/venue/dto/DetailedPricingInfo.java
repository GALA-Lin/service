package com.unlimited.sports.globox.model.venue.dto;

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
 * 详细价格信息 - 支持按场地分组的价格配置
 * 用于替代简单的时间->价格映射，支持不同场地在同一时间有不同价格的场景
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedPricingInfo {

    /**
     * 按场地ID分组的槽位价格映射
     * Key1: courtId (本地场地ID)
     * Key2: startTime (槽位开始时间)
     * Value: 价格
     *
     * 例如：
     * courtId=1: {10:00->100, 14:00->150}
     * courtId=2: {10:00->120, 14:00->180}
     */
    private Map<Long, Map<LocalTime, BigDecimal>> pricesByCourtId;

    /**
     * 基础价格总和（所有场地所有槽位的价格之和）
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
     * 每个场地的价格模板ID映射
     * 记录每个场地实际使用的价格模板ID，便于调试和审计
     */
    private Map<Long, Long> courtIdToTemplateIdMap;


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
         * 费用金额（该场地所有槽位的总金额）
         */
        private BigDecimal amount;

        /**
         * 单个槽位的费用金额
         */
        private BigDecimal perSlotAmount;
    }
}

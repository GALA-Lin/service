package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/**
 * 订单详情 - 响应载体类
 */
@Data
@Builder
public class GetOrderDetailsVo {

    @NotNull
    private Long orderNo;

    @NotNull
    private Integer sellerType;

    @NotNull
    private Long sellerId;

    @NotNull
    private String sellerName;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private Integer currentOrderStatus;

    @NotNull
    private List<ExtraChargeVo> orderLevelExtraCharges;

    /**
     * 订单项列表
     */
    @NotNull
    private List<OrderItemDetailVo> items;

    @Data
    @Builder
    public static class OrderItemDetailVo {

        @NotNull
        private Long orderItemId;

        @NotNull
        private Long resourceId;

        @NotNull
        private String resourceName;

        @NotNull
        private Long slotId;

        @NotNull
        private LocalDate bookingDate;

        @NotNull
        private BigDecimal itemBaseAmount;

        // item.subtotal
        @NotNull
        private BigDecimal itemAmount;

        @Null
        private List<ExtraChargeVo> extraCharges;

        @NotNull
        private RefundStatusEnum refundStatus;

        /**
         * 预定时间段
         */
        @NotNull
        private List<SlotBookingTime> slotBookingTimes;

        /**
         * 退款信息（没有则为 null）
         */
        @Null
        private ItemRefundVo refund;
    }

    @Data
    @Builder
    public static class ExtraChargeVo {
        @NotNull
        private Long chargeTypeId;
        @NotNull
        private String chargeName;
        @NotNull
        private Integer chargeMode;
        @NotNull
        private BigDecimal fixedValue;
        @NotNull
        private BigDecimal chargeAmount;
    }

    @Data
    @Builder
    public static class ItemRefundVo {
        @NotNull
        private BigDecimal refundAmount;
        @NotNull
        private BigDecimal refundFee;
        @NotNull
        private RefundStatusEnum refundStatus;
    }
}
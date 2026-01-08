package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2025/12/25 11:31
 * 订单取消结果Vo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelResultVo {

    /**
     * 操作是否成功
     */
    @NonNull
    private Boolean success;

    /**
     * 订单ID
     */
    @NonNull
    private Long orderId;

    /**
     * 订单号
     */
    @NonNull
    private String orderNo;

    /**
     * 取消类型：1=全部取消，2=部分取消
     */
    @NonNull
    private Integer cancelType;

    /**
     * 取消的时段数量
     */
    @NonNull
    private Integer cancelledSlotCount;

    /**
     * 剩余时段数量
     */
    @NonNull
    private Integer remainingSlotCount;

    /**
     * 退款金额
     */
    @NonNull
    private BigDecimal refundAmount;

    /**
     * 退款单号
     */
    private String refundNo;

    /**
     * 订单新状态
     */
    @NonNull
    private Integer orderStatus;

    /**
     * 订单新状态名称
     */
    private String orderStatusName;

    /**
     * 订单剩余金额
     */
    @NonNull
    private BigDecimal remainingAmount;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 已取消的时段ID列表
     */
    @NonNull
    private List<Long> cancelledSlotIds;

    /**
     * 取消时间
     */
    @NonNull
    private LocalDateTime cancelledAt;

    /**
     * 消息提示
     */
    private String message;

    /**
     * 退款详情
     */
    @NonNull
    private RefundDetailVo refundDetail;

    /**
     * 快速构建成功结果
     */
    public static OrderCancelResultVo success(String orderNo, Integer cancelType,
                                              Integer cancelledSlotCount, BigDecimal refundAmount) {
        return OrderCancelResultVo.builder()
                .success(true)
                .orderNo(orderNo)
                .cancelType(cancelType)
                .cancelledSlotCount(cancelledSlotCount)
                .refundAmount(refundAmount)
                .cancelledAt(LocalDateTime.now())
                .message("订单取消成功")
                .build();
    }

    /**
     * 快速构建失败结果
     */
    public static OrderCancelResultVo failure(String orderNo, String message) {
        return OrderCancelResultVo.builder()
                .success(false)
                .orderNo(orderNo)
                .message(message)
                .build();
    }

    /**
     * 退款详情内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundDetailVo {
        /**
         * 退款状态：1=处理中，2=成功，3=失败
         */
        @NonNull
        private Integer refundStatus;

        /**
         * 退款状态名称
         */
        private String refundStatusName;

        /**
         * 退款金额
         */
        @NonNull
        private BigDecimal refundAmount;

        /**
         * 退款单号
         */
        private String refundNo;

        /**
         * 退款时间
         */
        private LocalDateTime refundTime;

        /**
         * 预计到账时间
         */
        private String estimatedArrivalTime;

        /**
         * 退款说明
         */
        private String refundRemark;
    }
}
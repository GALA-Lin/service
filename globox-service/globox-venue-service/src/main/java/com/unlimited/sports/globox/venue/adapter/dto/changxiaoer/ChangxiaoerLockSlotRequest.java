package com.unlimited.sports.globox.venue.adapter.dto.changxiaoer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场小二锁场请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangxiaoerLockSlotRequest {

    /**
     * 日期范围
     */
    private DateRange dateRange;

    /**
     * 星期几
     */
    private String dayOfWeek;

    /**
     * 预订人信息
     */
    private Booker booker;

    /**
     * 价格模式
     */
    private String priceMode;

    /**
     * 场地订单列表
     */
    private List<PlaceOrder> placeOrders;

    /**
     * 支付信息
     */
    private PaymentInfo paymentInfo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 管理员ID
     */
    private String adminId;

    /**
     * 日期范围
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private String startDate;
        private String endDate;
    }

    /**
     * 预订人信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Booker {
        private PhoneNumber phoneNumber;
        private String bookerName;
    }

    /**
     * 电话号码
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneNumber {
        private String phoneNumber;
        private String countryCode;
    }

    /**
     * 场地订单
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceOrder {
        private Long placeId;
        private TimeRange timeRange;
        private Integer price;
        private List<Object> surchargeItems;
    }

    /**
     * 时间范围
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRange {
        private String startTime;
        private String endTime;
    }

    /**
     * 支付信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String paymentMethod;
        private Boolean paid;
    }
}

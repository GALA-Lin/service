package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wefitos时间槽位信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosTimeSlot {

    /**
     * 开始时间
     */
    private String start;

    /**
     * 结束时间
     */
    private String end;

    /**
     * 状态（1=可用, 3=已锁定, 4=已预订, 7=不可用）
     */
    private Integer status;

    /**
     * 时间戳（日期）
     */
    private Long date;

    /**
     * 价格信息
     */
    private WefitosPrice price;

    /**
     * 预订记录列表
     */
    private List<WefitosRecord> record;

    /**
     * 锁定信息
     */
    private WefitosLock lock;

    /**
     * 价格信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WefitosPrice {

        /**
         * 价格标题（星期几）
         */
        private String title;

        /**
         * 星期几（0-6）
         */
        private String weekday;

        /**
         * 会员价格
         */
        private String memberPrice;

        /**
         * 普通价格
         */
        private String price;

        /**
         * 永久会员价格
         */
        private String permanentPrice;
    }

    /**
     * 预订记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WefitosRecord {

        /**
         * 记录ID
         */
        @JsonProperty("_id")
        private WefitosId id;

        /**
         * 订单ID
         */
        private String subscribeId;

        /**
         * 用户名
         */
        private String name;

        /**
         * 电话
         */
        private String phone;

        /**
         * 价格
         */
        private Integer price;

        /**
         * 订单状态（1=已预订）
         */
        private Integer orderState;

        /**
         * 支付状态（2=已支付）
         */
        private Integer payState;

        /**
         * 备注
         */
        private String remark;
    }

    /**
     * 锁定信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WefitosLock {

        /**
         * 锁定ID
         */
        @JsonProperty("_id")
        private WefitosId id;

        /**
         * 锁定备注
         */
        private String remark;

        /**
         * 开始时间
         */
        private String startTime;

        /**
         * 结束时间
         */
        private String endTime;

        /**
         * 球场ID
         */
        private String courtId;

        /**
         * 创建时间戳
         */
        private Long insertTime;
    }

    /**
     * MongoDB ID包装
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WefitosId {

        /**
         * ID值
         */
        @JsonProperty("$id")
        private String id;
    }
}

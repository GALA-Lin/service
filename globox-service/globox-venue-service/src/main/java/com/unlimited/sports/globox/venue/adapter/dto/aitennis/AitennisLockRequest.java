package com.unlimited.sports.globox.venue.adapter.dto.aitennis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 爱网球锁场请求
 */
@Data
@Builder
public class AitennisLockRequest {

    /**
     * 租赁价格ID列表
     */
    @JsonProperty("rent_price_id")
    private List<String> rentPriceId;

    /**
     * 用途：20（锁场）
     */
    private Integer purpose;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否重复
     */
    @JsonProperty("is_duplicate")
    private Boolean isDuplicate;

    /**
     * 租赁价格信息列表
     */
    @JsonProperty("rent_price")
    private List<RentPriceInfo> rentPrice;

    /**
     * 租赁价格信息
     */
    @Data
    @Builder
    public static class RentPriceInfo {
        /**
         * 租赁价格ID
         */
        private String id;

        /**
         * 开始时间（格式：0700）
         */
        @JsonProperty("start_time")
        private String startTime;

        /**
         * 结束时间（格式：0800）
         */
        @JsonProperty("end_time")
        private String endTime;
    }
}

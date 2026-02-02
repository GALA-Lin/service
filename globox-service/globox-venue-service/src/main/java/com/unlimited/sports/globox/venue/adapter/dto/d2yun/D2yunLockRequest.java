package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * D2yun锁定槽位请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunLockRequest {

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;

    @JsonProperty("stadium_id")
    private Long stadiumId;

    /**
     * 要锁定的资源列表
     */
    private List<D2yunLockResourceItem> list;

    /**
     * 备注
     */
    private String remark;

    /**
     * 锁定资源项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class D2yunLockResourceItem {
        private String id;
        private String name;

        @JsonProperty("space_id")
        private Long spaceId;

        private Integer time;
        private Integer worth;
        private String unit;
        private Integer status;
        private String remark;

        @JsonProperty("resource_id")
        private Long resourceId;

        @JsonProperty("schedule_id")
        private Long scheduleId;

        @JsonProperty("order_id")
        private Long orderId;
    }
}

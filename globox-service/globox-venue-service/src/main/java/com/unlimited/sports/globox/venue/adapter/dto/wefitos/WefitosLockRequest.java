package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wefitos锁定槽位请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosLockRequest {

    /**
     * 球场项目ID
     */
    private String courtProjectId;

    /**
     * 球场周期列表
     */
    private List<CourtWeek> courtWeek;

    /**
     * 球场周期信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtWeek {

        /**
         * 球场名称
         */
        private String name;

        /**
         * 日期（YYYY-MM-DD）
         */
        private String date;

        /**
         * 星期几
         */
        private String day;

        /**
         * 球场ID
         */
        private String courtId;

        /**
         * 开始时间
         */
        private String startTime;

        /**
         * 结束时间
         */
        private String endTime;

        /**
         * 是否半场（0=全场）
         */
        private String half;

        /**
         * 备注
         */
        private String remark;
    }
}

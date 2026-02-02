package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wefitos资源列表响应数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosResourceListData {

    /**
     * 球场项目列表
     */
    private List<WefitosCourtProject> list;

    /**
     * 球场项目信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WefitosCourtProject {

        /**
         * 球场项目名称
         */
        private String name;

        /**
         * 球场项目ID
         */
        private String courtProjectId;

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
         * 时间槽位列表
         */
        private List<WefitosTimeSlot> timeList;
    }
}

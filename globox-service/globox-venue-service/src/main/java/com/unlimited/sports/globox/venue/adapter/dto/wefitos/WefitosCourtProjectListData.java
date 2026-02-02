package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wefitos获取球场项目列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosCourtProjectListData {

    /**
     * 项目列表
     */
    private List<CourtProject> list;

    /**
     * 总数
     */
    private Integer total;

    /**
     * 球场项目信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtProject {

        /**
         * 项目ID
         */
        @JsonProperty("_id")
        private ProjectId id;

        /**
         * 项目名称（时段信息，如"06:00-16:00"）
         */
        private String name;

        /**
         * 开始时间
         */
        private String startTime;

        /**
         * 结束时间
         */
        private String endTime;

        /**
         * 时长（以小时计）
         */
        private String duration;

        /**
         * 球场数量
         */
        private Integer courtsCount;
    }

    /**
     * MongoDB ID包装
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectId {

        /**
         * ID值
         */
        @JsonProperty("$id")
        private String id;
    }
}

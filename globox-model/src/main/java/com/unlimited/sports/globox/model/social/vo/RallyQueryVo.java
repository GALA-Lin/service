package com.unlimited.sports.globox.model.social.vo;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RallyQueryVo {

    @NonNull
    private List<DictItem> area;
    @NonNull
    private List<DictItem> timeRange;
    @NonNull
    private List<DictItem> genderLimit;
    @NonNull
    private List<DictItem> activityType;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DictItem {
        /**
         * 值（用于前端提交参数）
         */
        private Integer value;

        /**
         * 描述（用于前端显示）
         */
        private String description;
    }
}

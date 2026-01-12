package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量初始化场地时段模板结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTemplateInitResultVo {

    /**
     * 是否全部成功
     */
    private Boolean success;

    /**
     * 总场地数
     */
    private Integer totalCourts;

    /**
     * 成功初始化的场地数
     */
    private Integer successCourts;

    /**
     * 跳过的场地数（已有模板且不覆盖）
     */
    private Integer skippedCourts;

    /**
     * 失败的场地数
     */
    private Integer failedCourts;

    /**
     * 总共创建的模板数
     */
    private Integer totalTemplatesCreated;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 每个场地的详细结果
     */
    private List<CourtInitDetail> courtDetails;

    /**
     * 单个场地初始化详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtInitDetail {
        /**
         * 场地ID
         */
        private Long courtId;

        /**
         * 场地名称
         */
        private String courtName;

        /**
         * 状态：success-成功，skipped-跳过，failed-失败
         */
        private String status;

        /**
         * 创建的模板数量
         */
        private Integer templatesCreated;

        /**
         * 备注说明
         */
        private String remark;
    }
}
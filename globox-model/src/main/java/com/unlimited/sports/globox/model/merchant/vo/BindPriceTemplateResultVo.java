package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量绑定价格模板结果VO
 * @since 2026-01-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindPriceTemplateResultVo {

    /**
     * 操作是否完全成功
     */
    private Boolean allSuccess;

    /**
     * 总场地数
     */
    private Integer totalCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 跳过数量
     */
    private Integer skippedCount;

    /**
     * 绑定的模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 总体消息
     */
    private String message;

    /**
     * 每个场地的详细结果
     */
    @Builder.Default
    private List<CourtBindDetail> details = new ArrayList<>();

    /**
     * 场地绑定详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtBindDetail {
        /**
         * 场地ID
         */
        private Long courtId;

        /**
         * 场地名称
         */
        private String courtName;

        /**
         * 所属场馆ID
         */
        private Long venueId;

        /**
         * 所属场馆名称
         */
        private String venueName;

        /**
         * 绑定状态：success-成功，failed-失败，skipped-跳过
         */
        private String status;

        /**
         * 原模板ID（如果有）
         */
        private Long oldTemplateId;

        /**
         * 原模板名称（如果有）
         */
        private String oldTemplateName;

        /**
         * 新模板ID
         */
        private Long newTemplateId;

        /**
         * 新模板名称
         */
        private String newTemplateName;

        /**
         * 结果说明
         */
        private String remark;
    }

    /**
     * 快速构建全部成功的结果
     */
    public static BindPriceTemplateResultVo allSuccess(Long templateId, String templateName,
                                                       List<CourtBindDetail> details) {
        return BindPriceTemplateResultVo.builder()
                .allSuccess(true)
                .totalCount(details.size())
                .successCount(details.size())
                .failedCount(0)
                .skippedCount(0)
                .templateId(templateId)
                .templateName(templateName)
                .message(String.format("成功为 %d 个场地绑定价格模板", details.size()))
                .details(details)
                .build();
    }

    /**
     * 快速构建部分成功的结果
     */
    public static BindPriceTemplateResultVo partialSuccess(Long templateId, String templateName,
                                                           int successCount, int failedCount, int skippedCount,
                                                           List<CourtBindDetail> details) {
        int totalCount = successCount + failedCount + skippedCount;
        return BindPriceTemplateResultVo.builder()
                .allSuccess(false)
                .totalCount(totalCount)
                .successCount(successCount)
                .failedCount(failedCount)
                .skippedCount(skippedCount)
                .templateId(templateId)
                .templateName(templateName)
                .message(String.format("批量绑定完成，成功 %d 个，失败 %d 个，跳过 %d 个",
                        successCount, failedCount, skippedCount))
                .details(details)
                .build();
    }

    /**
     * 快速构建全部失败的结果
     */
    public static BindPriceTemplateResultVo allFailed(Long templateId, String templateName,
                                                      List<CourtBindDetail> details, String reason) {
        return BindPriceTemplateResultVo.builder()
                .allSuccess(false)
                .totalCount(details.size())
                .successCount(0)
                .failedCount(details.size())
                .skippedCount(0)
                .templateId(templateId)
                .templateName(templateName)
                .message(String.format("批量绑定失败：%s", reason))
                .details(details)
                .build();
    }
}
package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-19-09:32
 * @Description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotGenerationResultVo {

    /**
     * 是否成功
     */
    @NonNull
    private Boolean success;

    /**
     * 总共生成的天数
     */
    @NonNull
    private Integer totalDays;

    /**
     * 总共生成的时段数
     */
    @NonNull
    private Integer totalSlots;

    /**
     * 跳过的天数（已有时段或不营业）
     */
    @NonNull
    private Integer skippedDays;

    /**
     * 每天生成的时段数详情
     */
    private List<DailySlotInfo> dailySlotInfoList;

    /**
     * 消息
     */
    @NonNull
    private String message;

    /**
     * 每日时段信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySlotInfo {
        /**
         * 日期
         */
        @NonNull
        private LocalDate date;

        /**
         * 生成的时段数
         */
        @NonNull
        private Integer slotCount;

        /**
         * 状态：success=成功, skipped=跳过, closed=不营业
         */
        @NonNull
        private String status;

        /**
         * 备注
         */
        @NonNull
        private String remark;
    }
}
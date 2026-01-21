package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 活动创建结果VO
 * 包含完整的活动信息和占用的槽位信息，便于前端立即渲染
 *
 * @since 2025/12/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityCreationResultVo {

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 活动批次ID（用于标识同批创建的活动）
     */
    private Long merchantBatchId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动类型ID
     */
    private Long activityTypeId;

    /**
     * 活动类型描述
     */
    private String activityTypeDesc;

    /**
     * 活动日期
     */
    private LocalDate activityDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 场地ID
     */
    private Long courtId;

    /**
     * 场地名称
     */
    private String courtName;

    /**
     * 最大参与人数
     */
    private Integer maxParticipants;

    /**
     * 当前参与人数
     */
    private Integer currentParticipants;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 活动图片URL列表
     */
    private List<String> imageUrls;

    /**
     * 报名截止时间
     */
    private LocalDateTime registrationDeadline;

    /**
     * 组织者ID
     */
    private Long organizerId;

    /**
     * 组织者类型
     */
    private Integer organizerType;

    /**
     * 组织者名称
     */
    private String organizerName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 最低NTRP等级要求
     */
    private Double minNtrpLevel;

    /**
     * 活动状态
     */
    private Integer status;

    /**
     * 活动状态描述
     */
    private String statusDesc;

    /**
     * 占用的槽位列表（与 /venue-availability 接口返回格式一致）
     */
    private List<ActivitySlotVo> occupiedSlots;

    /**
     * 活动占用的槽位信息（简化版，用于快速渲染）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivitySlotVo {
        /**
         * 槽位模板ID
         */
        private Long templateId;

        /**
         * 槽位类型：2-活动槽位
         */
        private Integer slotType;

        /**
         * 开始时间
         */
        private LocalTime startTime;

        /**
         * 结束时间
         */
        private LocalTime endTime;

        /**
         * 价格
         */
        private BigDecimal price;

        /**
         * 是否可用（活动槽位根据报名情况判断）
         */
        private Boolean isAvailable;

        /**
         * 状态码
         */
        private Integer status;

        /**
         * 状态描述
         */
        private String statusDesc;

        /**
         * 活动ID
         */
        private Long activityId;

        /**
         * 活动批次ID
         */
        private Long merchantBatchId;
    }
}
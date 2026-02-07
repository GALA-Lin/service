package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 商家端活动详情VO
 * 包含活动基本信息和参与者列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantActivityDetailVo {

    /**
     * 活动ID
     */
    @NonNull
    private Long activityId;

    /**
     * 场馆ID
     */
    @NonNull
    private Long venueId;

    /**
     * 场地ID
     */
    @NonNull
    private Long courtId;

    /**
     * 场地名称
     */
    private String courtName;

    /**
     * 活动类型ID
     */
    @NonNull
    private Long activityTypeId;

    /**
     * 活动类型描述
     */
    @NonNull
    private String activityTypeDesc;

    /**
     * 活动名称
     */
    @NonNull
    private String activityName;

    /**
     * 活动图片列表
     */
    private List<String> imageUrls;

    /**
     * 活动日期
     */
    @NonNull
    private LocalDate activityDate;

    /**
     * 开始时间
     */
    @NonNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @NonNull
    private LocalTime endTime;

    /**
     * 最大参与人数
     */
    private Integer maxParticipants;

    /**
     * 单个用户最多允许报名几个名额（默认1个，null表示不限制）
     */
    private Integer maxQuotaPerUser;

    /**
     * 当前参与人数
     */
    @NonNull
    private Integer currentParticipants;

    /**
     * 单人价格
     */
    private BigDecimal unitPrice;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 报名截止时间
     */
    private LocalDateTime registrationDeadline;

    /**
     * 最低NTRP水平要求
     */
    private Double minNtrpLevel;

    /**
     * 活动状态
     */
    @NonNull
    private Integer status;

    /**
     * 参与者列表
     */
    @NonNull
    private List<ActivityParticipantInfoVo> participants;
}

package com.unlimited.sports.globox.model.venue.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 活动详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueActivityDetailVo {

    /**
     * 活动ID
     */
    @NonNull
    private Long activityId;

    /**
     * 活动名称
     */
    @NonNull
    private String activityName;

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
     * 场地名字
     */
    @NonNull
    private String courtName;

    /**
     * 场馆名称
     */
    @NonNull
    private String venueName;

    /**
     * NTRP等级要求
     */
    private Double minNtrpLevel;

    /**
     * 活动费用
     */
    private BigDecimal unitPrice;

    /**
     * 当前报名人数
     */
    @NonNull
    private Integer currentParticipants;

    /**
     * 最多报名人数
     */
    private Integer maxParticipants;


    /**
     * 活动状态
     */
    @NonNull
    private Integer status;
    /**
     * 已报名参与者列表
     */
    @NonNull
    @Builder.Default
    private List<ActivityParticipantVo> participants = new ArrayList<>();
}

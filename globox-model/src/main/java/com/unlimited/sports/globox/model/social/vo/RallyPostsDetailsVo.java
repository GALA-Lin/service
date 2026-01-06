package com.unlimited.sports.globox.model.social.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 约球帖子详情VO
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyPostsDetailsVo {
    /**
     * 约球帖子ID
     */
    private Long rallyPostId;

    /**
     * 约球发起人ID
     */
    private Long rallyInitiatorId;

    /**
     * 约球标题
     */
    private String rallyTitle;

    /**
     * 约球地区
     */
    private String rallyRegion;

    /**
     * 约球场馆名称
     */
    private String rallyVenueName;

    /**
     * 约球场地名称
     */
    private String rallyCourtName;

    /**
     * 约球活动日期
     */
    private LocalDate rallyEventDate;

    /**
     * 约球开始时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private LocalTime rallyStartTime;

    /**
     * 约球结束时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private LocalTime rallyEndTime;
    /**
     * 约球性别限制
     */
    private Integer rallyGenderLimit;

    /**
     * NTRP最低等级
     */
    private double ntrpMin;

    /**
     * NTRP最高等级
     */
    private double ntrpMax;

    /**
     * 约球参与人列表
     */
    private List<RallyParticipantVo> rallyParticipants;

    /**
     * 约球总人数
     */
    private Integer rallyTotalPeople;

    /**
     * 约球剩余人数
     */
    private Integer rallyRemainingPeople;

    /**
     * 约球状态
     */
    private Integer rallyStatus;

    /**
     * 费用
     */
    private BigDecimal rallyCost;

    /**
     * 承担方式: 0=发起人承担 1=AA分摊
     */
    private String rallyCostBearer;

    /**
     * 约球标签
     */
    private List<String> rallyLabel;


    /**
     *
     */
    private int rallyApplicationStatus;


    private boolean isOwner;

    private boolean isRallyCancel;

}

package com.unlimited.sports.globox.model.social.vo;

import com.unlimited.sports.globox.model.social.entity.RallyParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 约球帖子视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyPostsVo {

    /**
     * 约球帖子ID
     */
    private Long rallyPostId;

    /**
     * 约球发起人ID
     */
    private Long rallyInitiatorId;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 用户昵称
     */
    private String nickName;

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
    private LocalTime rallyTimeStart;

    /**
     * 约球结束时间
     */
    private LocalTime rallyTimeEnd;
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
     * 约球总人数
     */
    private Long rallyTotalPeople;

    /**
     * 约球剩余人数
     */
    private Long rallyRemainingPeople;
    
    /**
     * 约球状态
     */
    private int rallyStatus;
}
